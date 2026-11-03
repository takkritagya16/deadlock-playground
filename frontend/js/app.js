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
import { renderHistoryPanel } from './panels/history-panel.js';
import { renderGraph } from './graph/graph-view.js';

let currentSeq = null;

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
    window.getCurrentSeq = () => currentSeq;
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

        const hist = await getHistory();
        const events = hist && hist.events ? hist.events : (Array.isArray(hist) ? hist : []);
        if (events.length > 0) {
            currentSeq = events[events.length - 1].seq;
        } else {
            currentSeq = null;
        }
        renderHistoryPanel(events, currentSeq);
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
            currentSeq = null;
            renderHistoryPanel([], null);
        } else {
            const statusLine = document.getElementById('status-line');
            if (statusLine) {
                statusLine.textContent = err.message || err.code || 'An error occurred';
            }
            renderActionPanel(null, refresh);
            renderDetectRecoverPanel(null, refresh);
            currentSeq = null;
            renderHistoryPanel([], null);
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
    renderHistoryPanel([], null);
    refresh();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
