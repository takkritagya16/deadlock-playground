import { setPreventionToggles } from '../api/endpoints.js';

let initialized = false;
let refreshHandler = null;

let chkMutualExclusion = null;
let chkHoldAndWait = null;
let chkNoPreemption = null;
let chkCircularWait = null;
let errorLine = null;

let lastState = {
    mutualExclusion: false,
    holdAndWait: false,
    noPreemption: false,
    circularWait: false
};

function createPreventionSection(container) {
    const group = document.createElement('div');
    group.className = 'form-group prevention-controls';

    const heading = document.createElement('h3');
    heading.textContent = 'Prevention';
    group.appendChild(heading);

    const togglesList = document.createElement('div');
    togglesList.className = 'prevention-toggles';

    function createCheckbox(id, labelText) {
        const label = document.createElement('label');
        const input = document.createElement('input');
        input.type = 'checkbox';
        input.id = id;
        label.appendChild(input);
        const text = document.createTextNode(' ' + labelText);
        label.appendChild(text);
        togglesList.appendChild(label);
        return input;
    }

    chkMutualExclusion = createCheckbox('action-prev-mutualExclusion', 'Mutual exclusion');
    chkHoldAndWait = createCheckbox('action-prev-holdAndWait', 'Hold and wait');
    chkNoPreemption = createCheckbox('action-prev-noPreemption', 'No preemption');
    chkCircularWait = createCheckbox('action-prev-circularWait', 'Circular wait');

    group.appendChild(togglesList);

    errorLine = document.createElement('div');
    errorLine.id = 'action-prev-error';
    errorLine.className = 'error-line';
    group.appendChild(errorLine);

    async function handleToggleChange() {
        errorLine.textContent = '';
        const prevValues = { ...lastState };
        const newValues = {
            mutualExclusion: chkMutualExclusion.checked,
            holdAndWait: chkHoldAndWait.checked,
            noPreemption: chkNoPreemption.checked,
            circularWait: chkCircularWait.checked
        };

        try {
            await setPreventionToggles(newValues);
            lastState = { ...newValues };
            if (typeof refreshHandler === 'function') {
                await refreshHandler();
            }
        } catch (err) {
            const code = err.code || 'ERROR';
            const msg = err.message || 'Failed to update prevention toggles';
            errorLine.textContent = `${code}: ${msg}`;
            chkMutualExclusion.checked = prevValues.mutualExclusion;
            chkHoldAndWait.checked = prevValues.holdAndWait;
            chkNoPreemption.checked = prevValues.noPreemption;
            chkCircularWait.checked = prevValues.circularWait;
        }
    }

    chkMutualExclusion.addEventListener('change', handleToggleChange);
    chkHoldAndWait.addEventListener('change', handleToggleChange);
    chkNoPreemption.addEventListener('change', handleToggleChange);
    chkCircularWait.addEventListener('change', handleToggleChange);

    container.appendChild(group);
}

export function renderActionPanel(state, onRefresh) {
    if (typeof onRefresh === 'function') {
        refreshHandler = onRefresh;
    }

    const panel = document.getElementById('action-panel');
    if (!panel) {
        return;
    }

    const container = panel.querySelector('.panel-content') || panel;

    if (!initialized) {
        container.textContent = '';
        createPreventionSection(container);
        initialized = true;
    }

    const actualState = (state && state.state) ? state.state : state;
    const prev = (actualState && actualState.prevention) ? actualState.prevention : {};

    lastState = {
        mutualExclusion: Boolean(prev.mutualExclusion),
        holdAndWait: Boolean(prev.holdAndWait),
        noPreemption: Boolean(prev.noPreemption),
        circularWait: Boolean(prev.circularWait)
    };

    if (chkMutualExclusion) chkMutualExclusion.checked = lastState.mutualExclusion;
    if (chkHoldAndWait) chkHoldAndWait.checked = lastState.holdAndWait;
    if (chkNoPreemption) chkNoPreemption.checked = lastState.noPreemption;
    if (chkCircularWait) chkCircularWait.checked = lastState.circularWait;
}
