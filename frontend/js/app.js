import { getState } from './api/endpoints.js';
import { renderProcessPanel } from './panels/process-panel.js';
import { renderResourcePanel } from './panels/resource-panel.js';
import { renderStatusLine } from './panels/status-line.js';
import { renderConfigureForm } from './panels/configure-form.js';
import { renderGraph } from './graph/graph-view.js';

export async function refresh() {
    try {
        const result = await getState();
        renderStatusLine(result);
        renderProcessPanel(result);
        renderResourcePanel(result);
        renderGraph(result);
    } catch (err) {
        if (err.code === 'NO_SCENARIO') {
            const statusLine = document.getElementById('status-line');
            if (statusLine) {
                statusLine.textContent = 'No scenario loaded';
            }
            renderProcessPanel(null);
            renderResourcePanel(null);
            renderGraph(null);
        } else {
            const statusLine = document.getElementById('status-line');
            if (statusLine) {
                statusLine.textContent = err.message || err.code || 'An error occurred';
            }
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
    refresh();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
