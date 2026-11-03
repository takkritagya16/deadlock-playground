import {
    getState,
    configureScenario,
    reset,
    getHistory,
    requestResource,
    releaseResource,
    step,
    detect,
    recover,
    jumpToEvent,
    setPreventionToggles
} from './api/endpoints.js';
import { renderProcessPanel } from './panels/process-panel.js';
import { renderResourcePanel } from './panels/resource-panel.js';
import { renderStatusLine } from './panels/status-line.js';
import { renderConfigureForm } from './panels/configure-form.js';
import { renderActionPanel } from './panels/action-panel.js';
import { renderDetectRecoverPanel } from './panels/detect-recover-panel.js';
import { renderGraph } from './graph/graph-view.js';

if (typeof window !== 'undefined') {
    window.getState = getState;
    window.configureScenario = configureScenario;
    window.reset = reset;
    window.getHistory = getHistory;
    window.requestResource = requestResource;
    window.releaseResource = releaseResource;
    window.step = step;
    window.detect = detect;
    window.recover = recover;
    window.jumpToEvent = jumpToEvent;
    window.setPreventionToggles = setPreventionToggles;
    window.refresh = refresh;
}

export async function refresh() {
    try {
        const result = await getState();
        renderStatusLine(result);
        renderProcessPanel(result);
        renderResourcePanel(result);
        renderGraph(result);
        renderActionPanel(result, refresh);
        renderDetectRecoverPanel(result, refresh);
    } catch (err) {
        if (err.code === 'NO_SCENARIO') {
            const statusLine = document.getElementById('status-line');
            if (statusLine) {
                statusLine.textContent = 'No scenario loaded';
            }
            renderProcessPanel(null);
            renderResourcePanel(null);
            renderGraph(null);
            renderActionPanel(null, refresh);
            renderDetectRecoverPanel(null, refresh);
        } else {
            const statusLine = document.getElementById('status-line');
            if (statusLine) {
                statusLine.textContent = err.message || err.code || 'An error occurred';
            }
            renderActionPanel(null, refresh);
            renderDetectRecoverPanel(null, refresh);
        }
    }
}

export function init() {
    const configPanel = document.getElementById('configure-panel');
    if (configPanel) {
        renderConfigureForm(configPanel, async () => {
            await refresh();
        });
    }
    renderDetectRecoverPanel(null, refresh);
    refresh();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
