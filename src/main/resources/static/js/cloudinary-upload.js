/**
 * Shared Cloudinary Upload Logic
 * Handles file uploads to Cloudinary via unsigned preset.
 */

// Configuration defaults (can be overridden by global variables if needed)
const CLOUDINARY_DEFAULTS = {
    CLOUD_NAME: 'dugf7ukeo',
    UPLOAD_PRESET: 'app-ticket',
    API_URL: (cloudName) => `https://api.cloudinary.com/v1_1/${cloudName}/image/upload`,
    VIDEO_URL: (cloudName) => `https://api.cloudinary.com/v1_1/${cloudName}/video/upload`
};

/**
 * Uploads a single file to Cloudinary
 * @param {File} file - The file object to upload
 * @param {string} cloudName - Cloudinary Cloud Name
 * @param {string} uploadPreset - Cloudinary Upload Preset (Unsigned)
 * @returns {Promise<string>} - The secure URL of the uploaded file
 */
async function uploadToCloudinary(file, cloudName = CLOUDINARY_DEFAULTS.CLOUD_NAME, uploadPreset = CLOUDINARY_DEFAULTS.UPLOAD_PRESET) {
    if (!file) return null;

    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', uploadPreset);

    const url = file.type.startsWith('video/')
        ? CLOUDINARY_DEFAULTS.VIDEO_URL(cloudName)
        : CLOUDINARY_DEFAULTS.API_URL(cloudName);

    try {
        const response = await fetch(url, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(`Cloudinary Error: ${errorData.error ? errorData.error.message : response.statusText}`);
        }

        const data = await response.json();
        return data.secure_url;
    } catch (error) {
        console.error('Upload failed:', error);
        throw error;
    }
}

/**
 * Helper to show loading state on a button
 * @param {HTMLButtonElement} btn - The submit button
 * @param {string} loadingText - Text to show while loading
 */
function setButtonLoading(btn, loadingText = 'Đang xử lý...') {
    const originalText = btn.innerHTML;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> ${loadingText}`;
    btn.disabled = true;
    return () => {
        btn.innerHTML = originalText;
        btn.disabled = false;
    };
}
