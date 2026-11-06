#include "flycore.h"

static float channel_to_linear(unsigned int channel) {
    float c = (float) channel / 255.0f;
    if (c <= 0.04045f) {
        return c / 12.92f;
    }
    return (float) ((c + 0.055) / 1.055);
}

int fly_clamp_int(int value, int minimum, int maximum) {
    if (value < minimum) {
        return minimum;
    }
    if (value > maximum) {
        return maximum;
    }
    return value;
}

uint32_t fly_hash32(const uint32_t *data, size_t count, size_t stride) {
    uint32_t hash = 2166136261u;
    size_t step = stride == 0 ? 1 : stride;
    size_t index;
    if (data == NULL) {
        return hash;
    }
    for (index = 0; index < count; index += step) {
        uint32_t value = data[index];
        hash ^= (value & 0xFFu);
        hash *= 16777619u;
        hash ^= ((value >> 8) & 0xFFu);
        hash *= 16777619u;
        hash ^= ((value >> 16) & 0xFFu);
        hash *= 16777619u;
        hash ^= ((value >> 24) & 0xFFu);
        hash *= 16777619u;
    }
    return hash;
}

float fly_relative_luminance(uint32_t argb) {
    unsigned int r = (argb >> 16) & 0xFFu;
    unsigned int g = (argb >> 8) & 0xFFu;
    unsigned int b = argb & 0xFFu;
    return 0.2126f * channel_to_linear(r)
         + 0.7152f * channel_to_linear(g)
         + 0.0722f * channel_to_linear(b);
}

float fly_contrast_ratio(uint32_t foreground, uint32_t background) {
    float a = fly_relative_luminance(foreground);
    float b = fly_relative_luminance(background);
    float lighter = a > b ? a : b;
    float darker = a > b ? b : a;
    return (lighter + 0.05f) / (darker + 0.05f);
}

uint32_t fly_mix(uint32_t from, uint32_t to, float t) {
    float clamped = t < 0.0f ? 0.0f : (t > 1.0f ? 1.0f : t);
    int a = (int) (((from >> 24) & 0xFFu) + (float) (((int) ((to >> 24) & 0xFFu)) - ((int) ((from >> 24) & 0xFFu))) * clamped);
    int r = (int) (((from >> 16) & 0xFFu) + (float) (((int) ((to >> 16) & 0xFFu)) - ((int) ((from >> 16) & 0xFFu))) * clamped);
    int g = (int) (((from >> 8) & 0xFFu) + (float) (((int) ((to >> 8) & 0xFFu)) - ((int) ((from >> 8) & 0xFFu))) * clamped);
    int b = (int) ((from & 0xFFu) + (float) (((int) (to & 0xFFu)) - ((int) (from & 0xFFu))) * clamped);
    a = fly_clamp_int(a, 0, 255);
    r = fly_clamp_int(r, 0, 255);
    g = fly_clamp_int(g, 0, 255);
    b = fly_clamp_int(b, 0, 255);
    return ((uint32_t) a << 24) | ((uint32_t) r << 16) | ((uint32_t) g << 8) | (uint32_t) b;
}

int fly_dither_offset(uint32_t seed, int amplitude) {
    uint32_t mixed = seed;
    mixed ^= mixed >> 16;
    mixed *= 0x7FEB352Du;
    mixed ^= mixed >> 15;
    mixed *= 0x846CA68Bu;
    mixed ^= mixed >> 16;
    if (amplitude <= 0) {
        return 0;
    }
    return (int) (mixed % (uint32_t) (amplitude * 2 + 1)) - amplitude;
}
