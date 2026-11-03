import { request } from './client.js';

export async function getState() {
    return request('GET', '/api/state');
}

export async function configureScenario(scenarioBody) {
    return request('POST', '/api/scenario', scenarioBody);
}

export async function setPreventionToggles(body) {
    return request('POST', '/api/prevention', body);
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

export async function detect() {
    return request('POST', '/api/detect', {});
}

export async function recover(policy = 'RECOVERY_COST', strategy = 'TERMINATE') {
    return request('POST', '/api/recover', {
        policy: policy !== undefined ? policy : 'RECOVERY_COST',
        strategy: strategy !== undefined ? strategy : 'TERMINATE'
    });
}

export async function jumpToEvent(seq) {
    return request('POST', '/api/history/jump', { seq });
}
