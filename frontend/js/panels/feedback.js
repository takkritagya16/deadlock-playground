export function clearError(targetId) {
    const el = typeof targetId === 'string' ? document.getElementById(targetId) : targetId;
    if (el) {
        el.textContent = '';
    }
}

export function showError(targetId, error) {
    const el = typeof targetId === 'string' ? document.getElementById(targetId) : targetId;
    if (!el) {
        return;
    }

    let code = 'ERROR';
    let message = '';

    if (error) {
        if (typeof error === 'string') {
            message = error;
        } else {
            code = error.code || 'ERROR';
            message = error.message || String(error);
        }
    }

    el.textContent = `${code}: ${message}`;
}
