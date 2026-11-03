import {
    setPreventionToggles,
    requestResource,
    releaseResource,
    step
} from '../api/endpoints.js';

let initialized = false;
let refreshHandler = null;

let chkMutualExclusion = null;
let chkHoldAndWait = null;
let chkNoPreemption = null;
let chkCircularWait = null;
let prevErrorLine = null;

let procSelect = null;
let resSelect = null;
let countInput = null;
let btnRequest = null;
let btnRelease = null;
let btnStep = null;
let resultLine = null;

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

    prevErrorLine = document.createElement('div');
    prevErrorLine.id = 'action-prev-error';
    prevErrorLine.className = 'error-line';
    group.appendChild(prevErrorLine);

    async function handleToggleChange() {
        prevErrorLine.textContent = '';
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
            prevErrorLine.textContent = `${code}: ${msg}`;
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

function createActionSection(container) {
    const group = document.createElement('div');
    group.className = 'form-group simulation-actions';

    const heading = document.createElement('h3');
    heading.textContent = 'Operations';
    group.appendChild(heading);

    const controlsRow = document.createElement('div');
    controlsRow.className = 'action-inputs-row';

    const procLabel = document.createElement('label');
    procLabel.textContent = 'Process: ';
    procSelect = document.createElement('select');
    procSelect.id = 'action-proc-select';
    procLabel.appendChild(procSelect);
    controlsRow.appendChild(procLabel);

    const resLabel = document.createElement('label');
    resLabel.textContent = ' Resource: ';
    resSelect = document.createElement('select');
    resSelect.id = 'action-res-select';
    resLabel.appendChild(resSelect);
    controlsRow.appendChild(resLabel);

    const countLabel = document.createElement('label');
    countLabel.textContent = ' Count: ';
    countInput = document.createElement('input');
    countInput.type = 'number';
    countInput.id = 'action-count-input';
    countInput.value = '1';
    countInput.min = '1';
    countLabel.appendChild(countInput);
    controlsRow.appendChild(countLabel);

    group.appendChild(controlsRow);

    const btnRow = document.createElement('div');
    btnRow.className = 'action-buttons-row';

    btnRequest = document.createElement('button');
    btnRequest.type = 'button';
    btnRequest.id = 'btn-action-request';
    btnRequest.textContent = 'Request';
    btnRow.appendChild(btnRequest);

    btnRelease = document.createElement('button');
    btnRelease.type = 'button';
    btnRelease.id = 'btn-action-release';
    btnRelease.textContent = 'Release';
    btnRow.appendChild(btnRelease);

    btnStep = document.createElement('button');
    btnStep.type = 'button';
    btnStep.id = 'btn-action-step';
    btnStep.textContent = 'Step';
    btnRow.appendChild(btnStep);

    group.appendChild(btnRow);

    resultLine = document.createElement('div');
    resultLine.id = 'action-result-line';
    resultLine.className = 'action-result-line';
    group.appendChild(resultLine);

    btnRequest.addEventListener('click', async () => {
        resultLine.textContent = '';
        const process = procSelect.value;
        const resource = resSelect.value;
        const count = parseInt(countInput.value, 10) || 1;

        if (!process || !resource) {
            resultLine.textContent = 'Please select a process and a resource.';
            return;
        }

        try {
            const data = await requestResource(process, resource, count);
            if (typeof refreshHandler === 'function') {
                await refreshHandler();
            }
            if (data.reason) {
                resultLine.textContent = `${data.outcome}: ${data.reason}`;
            } else {
                resultLine.textContent = `${data.outcome}`;
            }
        } catch (err) {
            const code = err.code || 'ERROR';
            const msg = err.message || 'Request failed';
            resultLine.textContent = `${code}: ${msg}`;
        }
    });

    btnRelease.addEventListener('click', async () => {
        resultLine.textContent = '';
        const process = procSelect.value;
        const resource = resSelect.value;
        const count = parseInt(countInput.value, 10) || 1;

        if (!process || !resource) {
            resultLine.textContent = 'Please select a process and a resource.';
            return;
        }

        try {
            const data = await releaseResource(process, resource, count);
            if (typeof refreshHandler === 'function') {
                await refreshHandler();
            }
            if (data.unblocked && data.unblocked.length > 0) {
                resultLine.textContent = `Unblocked: ${data.unblocked.join(', ')}`;
            } else {
                resultLine.textContent = 'Unblocked: none';
            }
        } catch (err) {
            const code = err.code || 'ERROR';
            const msg = err.message || 'Release failed';
            resultLine.textContent = `${code}: ${msg}`;
        }
    });

    btnStep.addEventListener('click', async () => {
        resultLine.textContent = '';
        try {
            const data = await step();
            if (typeof refreshHandler === 'function') {
                await refreshHandler();
            }
            const desc = data.executed ? data.executed.description : '';
            if (data.finished) {
                btnStep.disabled = true;
                const topStep = document.getElementById('btn-step');
                if (topStep) topStep.disabled = true;
                resultLine.textContent = desc ? `${desc} (Scenario finished)` : 'Scenario finished';
            } else {
                resultLine.textContent = desc || 'Step executed';
            }
        } catch (err) {
            const code = err.code || 'ERROR';
            const msg = err.message || 'Step failed';
            resultLine.textContent = `${code}: ${msg}`;
        }
    });

    const topBtnRequest = document.getElementById('btn-request');
    if (topBtnRequest && !topBtnRequest.dataset.wired) {
        topBtnRequest.dataset.wired = 'true';
        topBtnRequest.addEventListener('click', () => {
            if (btnRequest) btnRequest.click();
        });
    }

    const topBtnRelease = document.getElementById('btn-release');
    if (topBtnRelease && !topBtnRelease.dataset.wired) {
        topBtnRelease.dataset.wired = 'true';
        topBtnRelease.addEventListener('click', () => {
            if (btnRelease) btnRelease.click();
        });
    }

    const topBtnStep = document.getElementById('btn-step');
    if (topBtnStep && !topBtnStep.dataset.wired) {
        topBtnStep.dataset.wired = 'true';
        topBtnStep.addEventListener('click', () => {
            if (btnStep) btnStep.click();
        });
    }

    container.appendChild(group);
}

function updateSelectOptions(selectEl, items, idKey) {
    if (!selectEl) {
        return;
    }
    const currentVal = selectEl.value;
    selectEl.textContent = '';

    for (const item of items) {
        const id = item[idKey];
        const opt = document.createElement('option');
        opt.value = id;
        opt.textContent = id;
        selectEl.appendChild(opt);
    }

    if (currentVal && items.some(i => i[idKey] === currentVal)) {
        selectEl.value = currentVal;
    }
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
        createActionSection(container);
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

    const processes = (actualState && Array.isArray(actualState.processes)) ? actualState.processes : [];
    const resources = (actualState && Array.isArray(actualState.resources)) ? actualState.resources : [];

    updateSelectOptions(procSelect, processes, 'id');
    updateSelectOptions(resSelect, resources, 'id');

    if (actualState && Array.isArray(actualState.processes)) {
        const totalPending = actualState.processes.reduce((sum, p) => sum + (p.pendingOperations || 0), 0);
        if (totalPending > 0) {
            if (btnStep) btnStep.disabled = false;
            const topStep = document.getElementById('btn-step');
            if (topStep) topStep.disabled = false;
        }
    }
}
