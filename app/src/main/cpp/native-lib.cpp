#include <jni.h>

#include <android/log.h>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <vector>

#include "flycore.h"

#define FLY_LOG_TAG "FlyCore"
#define FLY_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, FLY_LOG_TAG, __VA_ARGS__)

namespace {

constexpr int kMaxPalette = 8;
constexpr int kBucketBits = 4;
constexpr int kBucketCount = 1 << (kBucketBits * 3);

inline int channel(uint32_t argb, int shift) {
    return static_cast<int>((argb >> shift) & 0xFFu);
}

inline uint32_t pack_argb(int a, int r, int g, int b) {
    return (static_cast<uint32_t>(fly_clamp_int(a, 0, 255)) << 24) |
           (static_cast<uint32_t>(fly_clamp_int(r, 0, 255)) << 16) |
           (static_cast<uint32_t>(fly_clamp_int(g, 0, 255)) << 8) |
           static_cast<uint32_t>(fly_clamp_int(b, 0, 255));
}

inline uint32_t opaque(uint32_t argb) {
    return (argb & 0x00FFFFFFu) | 0xFF000000u;
}

inline float clamp01(float value) {
    return value < 0.0f ? 0.0f : (value > 1.0f ? 1.0f : value);
}

float gradient_position(int x, int y, int width, int height, int style) {
    const float fx = width > 1 ? static_cast<float>(x) / static_cast<float>(width - 1) : 0.0f;
    const float fy = height > 1 ? static_cast<float>(y) / static_cast<float>(height - 1) : 0.0f;
    switch (style) {
        case 1: {
            // Radial glow anchored slightly above the centre.
            const float dx = (fx - 0.5f) * 1.15f;
            const float dy = (fy - 0.42f) * 0.72f;
            const float distance = std::sqrt(dx * dx + dy * dy);
            return clamp01(distance * 1.55f);
        }
        case 2: {
            // Soft diagonal mesh.
            const float wave = std::sin((fx * 3.4f + fy * 2.1f) * 3.14159265f);
            return clamp01(0.5f + 0.5f * wave * 0.72f + (fy - 0.5f) * 0.35f);
        }
        case 3: {
            // Structured vertical bands with a gentle horizontal lean.
            const float band = std::floor(fy * 6.0f) / 6.0f;
            return clamp01(band * 0.82f + fx * 0.18f);
        }
        case 4: {
            // Angular sweep around the centre.
            const float dx = fx - 0.5f;
            const float dy = fy - 0.5f;
            float angle = std::atan2(dy, dx) / (2.0f * 3.14159265f) + 0.5f;
            if (angle < 0.0f) {
                angle += 1.0f;
            }
            return clamp01(angle);
        }
        case 5: {
            // Two symmetric glows that fade toward the edges.
            const float ax = fx - 0.28f;
            const float ay = fy - 0.32f;
            const float bx = fx - 0.74f;
            const float by = fy - 0.72f;
            const float first = std::sqrt(ax * ax + ay * ay);
            const float second = std::sqrt(bx * bx + by * by);
            return clamp01(0.5f + 0.5f * (first - second) * 2.1f);
        }
        case 0:
        default:
            // Diagonal sweep.
            return clamp01((fx * 0.58f + fy * 0.82f) / 1.40f);
    }
}

uint32_t sample_palette(const uint32_t *colors, int count, float position) {
    if (count <= 0) {
        return 0xFF000000u;
    }
    if (count == 1) {
        return colors[0];
    }
    const float scaled = position * static_cast<float>(count - 1);
    int index = static_cast<int>(std::floor(scaled));
    if (index < 0) {
        index = 0;
    }
    if (index >= count - 1) {
        return colors[count - 1];
    }
    const float fraction = scaled - static_cast<float>(index);
    return fly_mix(colors[index], colors[index + 1], fraction);
}

}  // namespace

extern "C" {

JNIEXPORT void JNICALL
Java_com_fly_customizer_util_NativeFly_generateGradient(JNIEnv *env, jobject /*thiz*/,
                                                       jintArray pixels, jint width, jint height,
                                                       jintArray palette, jint style, jint grain) {
    if (pixels == nullptr || width <= 0 || height <= 0) {
        return;
    }
    if (env->GetArrayLength(pixels) < width * height) {
        FLY_LOGE("pixel buffer too small: %d < %d", env->GetArrayLength(pixels), width * height);
        return;
    }

    uint32_t colors[kMaxPalette];
    int colorCount = 0;
    if (palette != nullptr) {
        const jsize paletteLength = env->GetArrayLength(palette);
        colorCount = paletteLength < kMaxPalette ? static_cast<int>(paletteLength) : kMaxPalette;
        if (colorCount > 0) {
            jint *rawPalette = env->GetIntArrayElements(palette, nullptr);
            if (rawPalette != nullptr) {
                for (int i = 0; i < colorCount; ++i) {
                    colors[i] = opaque(static_cast<uint32_t>(rawPalette[i]));
                }
                env->ReleaseIntArrayElements(palette, rawPalette, JNI_ABORT);
            } else {
                colorCount = 0;
            }
        }
    }

    jint *destination = env->GetIntArrayElements(pixels, nullptr);
    if (destination == nullptr) {
        return;
    }

    const int amplitude = fly_clamp_int(static_cast<int>(grain), 0, 24);
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            const float position = gradient_position(x, y, width, height, style);
            const uint32_t base = sample_palette(colors, colorCount, position);
            int noise = 0;
            if (amplitude > 0) {
                const uint32_t seed = static_cast<uint32_t>(y) * 73856093u ^
                                      static_cast<uint32_t>(x) * 19349663u ^
                                      static_cast<uint32_t>(amplitude) * 83492791u;
                noise = fly_dither_offset(seed, amplitude);
            }
            const uint32_t shaded = pack_argb(255, channel(base, 16) + noise,
                                              channel(base, 8) + noise, channel(base, 0) + noise);
            destination[y * width + x] = static_cast<jint>(shaded);
        }
    }

    env->ReleaseIntArrayElements(pixels, destination, 0);
}

JNIEXPORT void JNICALL
Java_com_fly_customizer_util_NativeFly_boxBlur(JNIEnv *env, jobject /*thiz*/, jintArray pixels,
                                               jint width, jint height, jint radius) {
    if (pixels == nullptr || width <= 1 || height <= 1 || radius <= 0) {
        return;
    }
    const int limit = fly_clamp_int(static_cast<int>(radius), 1, 48);
    if (env->GetArrayLength(pixels) < width * height) {
        return;
    }
    jint *buffer = env->GetIntArrayElements(pixels, nullptr);
    if (buffer == nullptr) {
        return;
    }

    std::vector<uint32_t> source(static_cast<size_t>(width) * height);
    std::vector<uint32_t> scratch(source.size());
    for (size_t i = 0; i < source.size(); ++i) {
        source[i] = static_cast<uint32_t>(buffer[i]);
    }

    // Horizontal pass.
    for (int y = 0; y < height; ++y) {
        int sumR = 0;
        int sumG = 0;
        int sumB = 0;
        int count = 0;
        const int row = y * width;
        for (int x = -limit; x <= limit && x < width; ++x) {
            if (x >= 0) {
                const uint32_t pixel = source[row + x];
                sumR += channel(pixel, 16);
                sumG += channel(pixel, 8);
                sumB += channel(pixel, 0);
                ++count;
            }
        }
        for (int x = 0; x < width; ++x) {
            scratch[row + x] = pack_argb(255, count ? sumR / count : 0, count ? sumG / count : 0,
                                         count ? sumB / count : 0);
            const int removeIndex = x - limit;
            const int addIndex = x + limit + 1;
            if (removeIndex >= 0) {
                const uint32_t pixel = source[row + removeIndex];
                sumR -= channel(pixel, 16);
                sumG -= channel(pixel, 8);
                sumB -= channel(pixel, 0);
                --count;
            }
            if (addIndex < width) {
                const uint32_t pixel = source[row + addIndex];
                sumR += channel(pixel, 16);
                sumG += channel(pixel, 8);
                sumB += channel(pixel, 0);
                ++count;
            }
        }
    }

    // Vertical pass.
    for (int x = 0; x < width; ++x) {
        int sumR = 0;
        int sumG = 0;
        int sumB = 0;
        int count = 0;
        for (int y = -limit; y <= limit && y < height; ++y) {
            if (y >= 0) {
                const uint32_t pixel = scratch[y * width + x];
                sumR += channel(pixel, 16);
                sumG += channel(pixel, 8);
                sumB += channel(pixel, 0);
                ++count;
            }
        }
        for (int y = 0; y < height; ++y) {
            const uint32_t blended =
                    pack_argb(255, count ? sumR / count : channel(source[y * width + x], 16),
                              count ? sumG / count : channel(source[y * width + x], 8),
                              count ? sumB / count : channel(source[y * width + x], 0));
            const uint32_t original = source[y * width + x];
            // Keep 35% of the original detail so the gradient stays crisp.
            buffer[y * width + x] =
                    static_cast<jint>(fly_mix(blended, original, 0.35f));
            const int removeIndex = y - limit;
            const int addIndex = y + limit + 1;
            if (removeIndex >= 0) {
                const uint32_t pixel = scratch[removeIndex * width + x];
                sumR -= channel(pixel, 16);
                sumG -= channel(pixel, 8);
                sumB -= channel(pixel, 0);
                --count;
            }
            if (addIndex < height) {
                const uint32_t pixel = scratch[addIndex * width + x];
                sumR += channel(pixel, 16);
                sumG += channel(pixel, 8);
                sumB += channel(pixel, 0);
                ++count;
            }
        }
    }

    env->ReleaseIntArrayElements(pixels, buffer, 0);
}

JNIEXPORT jfloat JNICALL
Java_com_fly_customizer_util_NativeFly_contrastRatio(JNIEnv * /*env*/, jobject /*thiz*/,
                                                     jint foreground, jint background) {
    return static_cast<jfloat>(
            fly_contrast_ratio(static_cast<uint32_t>(foreground), static_cast<uint32_t>(background)));
}

JNIEXPORT jfloat JNICALL
Java_com_fly_customizer_util_NativeFly_luminance(JNIEnv * /*env*/, jobject /*thiz*/, jint color) {
    return static_cast<jfloat>(fly_relative_luminance(static_cast<uint32_t>(color)));
}

JNIEXPORT jintArray JNICALL
Java_com_fly_customizer_util_NativeFly_dominantColors(JNIEnv *env, jobject /*thiz*/,
                                                      jintArray pixels, jint maxColors) {
    if (pixels == nullptr) {
        return env->NewIntArray(0);
    }
    const jsize length = env->GetArrayLength(pixels);
    const int limit = fly_clamp_int(static_cast<int>(maxColors), 1, kMaxPalette);
    if (length <= 0) {
        return env->NewIntArray(0);
    }

    jint *buffer = env->GetIntArrayElements(pixels, nullptr);
    if (buffer == nullptr) {
        return env->NewIntArray(0);
    }

    std::vector<uint32_t> histogram(kBucketCount, 0);
    std::vector<uint32_t> sums(kBucketCount * 3, 0);
    const int stride = length > 200000 ? 7 : 3;
    for (jsize i = 0; i < length; i += stride) {
        const uint32_t pixel = static_cast<uint32_t>(buffer[i]) | 0xFF000000u;
        const int r = channel(pixel, 16) >> (8 - kBucketBits);
        const int g = channel(pixel, 8) >> (8 - kBucketBits);
        const int b = channel(pixel, 0) >> (8 - kBucketBits);
        const int index = (r << (kBucketBits * 2)) | (g << kBucketBits) | b;
        histogram[index] += 1;
        sums[index * 3] += static_cast<uint32_t>(channel(pixel, 16));
        sums[index * 3 + 1] += static_cast<uint32_t>(channel(pixel, 8));
        sums[index * 3 + 2] += static_cast<uint32_t>(channel(pixel, 0));
    }
    env->ReleaseIntArrayElements(pixels, buffer, JNI_ABORT);

    // Selection sort over bucket counts, skipping near-duplicate colours.
    std::vector<int> chosen;
    std::vector<bool> used(kBucketCount, false);
    for (int pick = 0; pick < limit; ++pick) {
        int bestIndex = -1;
        uint32_t bestCount = 0;
        for (int i = 0; i < kBucketCount; ++i) {
            if (used[i]) {
                continue;
            }
            if (histogram[i] > bestCount) {
                bestCount = histogram[i];
                bestIndex = i;
            }
        }
        if (bestIndex < 0 || bestCount == 0) {
            break;
        }
        used[bestIndex] = true;
        const uint32_t count = histogram[bestIndex];
        const uint32_t r = sums[bestIndex * 3] / count;
        const uint32_t g = sums[bestIndex * 3 + 1] / count;
        const uint32_t b = sums[bestIndex * 3 + 2] / count;
        const uint32_t color = pack_argb(255, static_cast<int>(r), static_cast<int>(g),
                                         static_cast<int>(b));
        chosen.push_back(static_cast<int>(color));
    }

    jintArray result = env->NewIntArray(static_cast<jsize>(chosen.size()));
    if (result != nullptr && !chosen.empty()) {
        env->SetIntArrayRegion(result, 0, static_cast<jsize>(chosen.size()),
                               reinterpret_cast<const jint *>(chosen.data()));
    }
    return result;
}

JNIEXPORT jint JNICALL
Java_com_fly_customizer_util_NativeFly_paletteHash(JNIEnv *env, jobject /*thiz*/,
                                                   jintArray pixels) {
    if (pixels == nullptr) {
        return 0;
    }
    const jsize length = env->GetArrayLength(pixels);
    if (length <= 0) {
        return 0;
    }
    jint *buffer = env->GetIntArrayElements(pixels, nullptr);
    if (buffer == nullptr) {
        return 0;
    }
    const uint32_t hash = fly_hash32(reinterpret_cast<const uint32_t *>(buffer),
                                     static_cast<size_t>(length), 97);
    env->ReleaseIntArrayElements(pixels, buffer, JNI_ABORT);
    return static_cast<jint>(hash);
}

JNIEXPORT jstring JNICALL
Java_com_fly_customizer_util_NativeFly_version(JNIEnv *env, jobject /*thiz*/) {
    return env->NewStringUTF("FlyCore 1.0 (C++17 + C11)");
}

}  // extern "C"
