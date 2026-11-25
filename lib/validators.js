/**
 * Input validation helpers for AAILA API endpoints.
 */

const FAMILY_ID_PATTERN = /^[A-Za-z0-9-]{4,24}$/;

export function validateFamilyId(familyId) {
  if (!familyId || typeof familyId !== "string") {
    return "Family ID is required.";
  }
  const trimmed = familyId.trim();
  if (!FAMILY_ID_PATTERN.test(trimmed)) {
    return "Family ID must be 4–24 characters using letters, numbers, or hyphens.";
  }
  return null;
}

export function validatePassword(password) {
  if (!password || typeof password !== "string") {
    return "Password is required.";
  }
  if (password.length < 6) {
    return "Password must be at least 6 characters.";
  }
  if (password.length > 128) {
    return "Password must not exceed 128 characters.";
  }
  return null;
}

export function validateName(name, label = "Name") {
  if (!name || typeof name !== "string") {
    return `${label} is required.`;
  }
  if (name.trim().length < 2) {
    return `${label} must be at least 2 characters.`;
  }
  if (name.trim().length > 100) {
    return `${label} must not exceed 100 characters.`;
  }
  return null;
}

export function collectErrors(validations) {
  const errors = validations.filter(Boolean);
  return errors.length > 0 ? errors : null;
}
