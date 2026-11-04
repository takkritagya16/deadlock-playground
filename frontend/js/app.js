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
import { renderConfigureForm } from './panels/configure-form.js';
import { renderActionPanel } from './panels/action-panel.js';
import { renderDetectRecoverPanel } from './panels/detect-recover-panel.js';
import { renderHistoryPanel } from './panels/history-panel.js';
import { renderGraph } from './graph/graph-view.js';

let currentSeq = null;
let currentEvents = [];

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
    window.jump = jump;
    window.getCurrentSeq = () => currentSeq;
}

function updateHistoryButtons(events, seq) {
    const btnBack = document.getElementById('btn-step-back');
    const btnForward = document.getElementById('btn-step-forward');
    if (!btnBack && !btnForward) return;

    if (!events || events.length === 0 || seq === null || seq === undefined) {
        if (btnBack) btnBack.disabled = true;
        if (btnForward) btnForward.disabled = true;
        return;
    }

    const hasPrev = events.some(e => e.seq === seq - 1);
    const hasNext = events.some(e => e.seq === seq + 1);

    if (btnBack) btnBack.disabled = !hasPrev;
    if (btnForward) btnForward.disabled = !hasNext;
}

export async function jump(seq) {
    try {
        const data = await jumpToEvent(seq);
        const jumpedState = (data && data.state) ? data.state : data;
        currentSeq = seq;

        renderProcessPanel(jumpedState);
        renderResourcePanel(jumpedState);
        renderGraph(jumpedState);
        renderActionPanel(jumpedState, refresh);
        renderDetectRecoverPanel(jumpedState, refresh);

        const hist = await getHistory();
        currentEvents = hist && hist.events ? hist.events : (Array.isArray(hist) ? hist : []);
        renderHistoryPanel(currentEvents, currentSeq, jump);
        updateHistoryButtons(currentEvents, currentSeq);
        return jumpedState;
    } catch (err) {
        console.error('Jump failed', err);
    }
}

export async function refresh() {
    try {
        const result = await getState();
        renderProcessPanel(result);
        renderResourcePanel(result);
        renderGraph(result);
        renderActionPanel(result, refresh);
        renderDetectRecoverPanel(result, refresh);

        const hist = await getHistory();
        currentEvents = hist && hist.events ? hist.events : (Array.isArray(hist) ? hist : []);
        if (currentEvents.length > 0) {
            currentSeq = currentEvents[currentEvents.length - 1].seq;
        } else {
            currentSeq = null;
        }
        renderHistoryPanel(currentEvents, currentSeq, jump);
        updateHistoryButtons(currentEvents, currentSeq);
    } catch (err) {
        if (err.code === 'NO_SCENARIO') {
            renderProcessPanel(null);
            renderResourcePanel(null);
            renderGraph(null);
            renderActionPanel(null, refresh);
            renderDetectRecoverPanel(null, refresh);
            currentSeq = null;
            currentEvents = [];
            renderHistoryPanel([], null, jump);
            updateHistoryButtons([], null);
        } else {
            renderActionPanel(null, refresh);
            renderDetectRecoverPanel(null, refresh);
            currentSeq = null;
            currentEvents = [];
            renderHistoryPanel([], null, jump);
            updateHistoryButtons([], null);
        }
    }
}

function setupHistoryControls() {
    const btnBack = document.getElementById('btn-step-back');
    if (btnBack && !btnBack.dataset.wired) {
        btnBack.dataset.wired = 'true';
        btnBack.addEventListener('click', async () => {
            if (currentSeq !== null && currentEvents.some(e => e.seq === currentSeq - 1)) {
                await jump(currentSeq - 1);
            }
        });
    }

    const btnForward = document.getElementById('btn-step-forward');
    if (btnForward && !btnForward.dataset.wired) {
        btnForward.dataset.wired = 'true';
        btnForward.addEventListener('click', async () => {
            if (currentSeq !== null && currentEvents.some(e => e.seq === currentSeq + 1)) {
                await jump(currentSeq + 1);
            }
        });
    }
}

export function init() {
    const historyOverlay = document.getElementById('history-overlay');
    const historyButton = document.getElementById('btn-history');
    const historyClose = document.getElementById('btn-history-close');
    historyButton?.addEventListener('click', () => {
        historyOverlay.hidden = false;
        historyClose.focus();
    });
    historyClose?.addEventListener('click', () => {
        historyOverlay.hidden = true;
        historyButton.focus();
    });
    historyOverlay?.addEventListener('click', (event) => {
        if (event.target === historyOverlay) historyClose.click();
    });
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && historyOverlay && !historyOverlay.hidden) historyClose.click();
    });

    document.getElementById('btn-configure')?.addEventListener('click', () => {
        document.getElementById('form-configure')?.requestSubmit();
    });

    const configPanel = document.getElementById('configure-panel');
    if (configPanel) {
        renderConfigureForm(configPanel, async () => {
            await refresh();
        });
    }
    renderDetectRecoverPanel(null, refresh);
    renderHistoryPanel([], null, jump);
    setupHistoryControls();
    updateHistoryButtons([], null);
    refresh();
}

if (typeof document !== 'undefined') {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
}
