import { request } from './client.js';

export async function getState() {
    return request('GET', '/api/state');
}

export async function configureScenario(scenarioBody) {
    return request('POST', '/api/scenario', scenarioBody);
}

export async function reset(confirmed = false) {
    return request('POST', '/api/reset', { confirmed });
}

export async function getHistory() {
    return request('GET', '/api/history');
}

export async function requestResource(process, resource, count) {
    return request('POST', '/api/request', { process, resource, count });
}

export async function releaseResource(process, resource, count) {
    return request('POST', '/api/release', { process, resource, count });
}

export async function step() {
    return request('POST', '/api/step', {});
}
