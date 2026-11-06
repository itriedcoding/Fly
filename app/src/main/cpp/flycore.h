#ifndef FLY_CORE_H
#define FLY_CORE_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* 32-bit FNV-1a style hash over a strided ARGB buffer. */
uint32_t fly_hash32(const uint32_t *data, size_t count, size_t stride);

/* WCAG relative luminance of an opaque ARGB colour. */
float fly_relative_luminance(uint32_t argb);

/* WCAG contrast ratio between two opaque ARGB colours (1.0 .. 21.0). */
float fly_contrast_ratio(uint32_t foreground, uint32_t background);

/* Linear ARGB interpolation, t clamped to 0..1. */
uint32_t fly_mix(uint32_t from, uint32_t to, float t);

int fly_clamp_int(int value, int minimum, int maximum);

/* Deterministic per-pixel dither offset in [-amplitude, +amplitude]. */
int fly_dither_offset(uint32_t seed, int amplitude);

#ifdef __cplusplus
}
#endif

#endif /* FLY_CORE_H */
