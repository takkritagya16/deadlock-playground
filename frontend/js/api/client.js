export class ApiError extends Error {
    constructor(code, message, status) {
        super(message || code);
        this.name = 'ApiError';
        this.code = code;
        this.message = message || code;
        this.status = status;
    }
}

export async function request(method, path, body = undefined) {
    const options = { method };
    if (body !== undefined && body !== null) {
        options.headers = { 'Content-Type': 'application/json' };
        options.body = typeof body === 'string' ? body : JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(path, options);
    } catch (err) {
        throw new ApiError('NETWORK_ERROR', err.message || 'Network request failed', 0);
    }

    let json;
    try {
        json = await response.json();
    } catch (err) {
        throw new ApiError('INTERNAL_ERROR', 'Failed to parse JSON response', response.status);
    }

    if (json && json.ok === true) {
        return json.data;
    }

    const errorData = (json && json.error) || {};
    throw new ApiError(
        errorData.code || 'UNKNOWN_ERROR',
        errorData.message || response.statusText || 'Request failed',
        response.status
    );
}
