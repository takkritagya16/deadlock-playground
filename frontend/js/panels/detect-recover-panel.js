import { detect, recover, reset } from '../api/endpoints.js';

let initialized = false;
let refreshHandler = null;

let btnDetect = null;
let detectResultLine = null;
let detectErrorLine = null;

let policySelect = null;
let strategySelect = null;
let btnRecover = null;
let recoverResultLine = null;
let recoverErrorLine = null;

let btnReset = null;
let resetErrorLine = null;

function createDetectSection(container) {
    const group = document.createElement('div');
    group.className = 'form-group detect-group';

    const heading = document.createElement('h3');
    heading.textContent = 'Deadlock Detection';
    group.appendChild(heading);

    const btnRow = document.createElement('div');
    btnRow.className = 'action-buttons-row';

    btnDetect = document.createElement('button');
    btnDetect.type = 'button';
    btnDetect.id = 'btn-action-detect';
    btnDetect.textContent = 'Detect';
    btnRow.appendChild(btnDetect);
    group.appendChild(btnRow);

    detectResultLine = document.createElement('div');
    detectResultLine.id = 'detect-result-line';
    detectResultLine.className = 'action-result-line';
    group.appendChild(detectResultLine);

    detectErrorLine = document.createElement('div');
    detectErrorLine.id = 'detect-error-line';
    detectErrorLine.className = 'error-line';
    group.appendChild(detectErrorLine);

    btnDetect.addEventListener('click', async () => {
        detectErrorLine.textContent = '';
        detectResultLine.textContent = '';
        try {
            const data = await detect();
            if (!data.deadlocked || !data.processes || data.processes.length === 0) {
                detectResultLine.textContent = 'No deadlock found';
            } else if (data.cycle && Array.isArray(data.cycle) && data.cycle.length > 0) {
                const procList = data.processes.join(', ');
                const cycleList = data.cycle.join(', ');
                detectResultLine.textContent = `Deadlock: processes ${procList}; cycle ${cycleList}`;
            } else {
                const procList = data.processes.join(', ');
                detectResultLine.textContent = `Deadlock: processes ${procList}`;
            }

            if (typeof refreshHandler === 'function') {
                await refreshHandler();
            }
        } catch (err) {
            const code = err.code || 'ERROR';
            const msg = err.message || 'Detect failed';
            detectErrorLine.textContent = `${code}: ${msg}`;
        }
    });

    const topBtnDetect = document.getElementById('btn-detect');
    if (topBtnDetect && !topBtnDetect.dataset.wired) {
        topBtnDetect.dataset.wired = 'true';
        topBtnDetect.addEventListener('click', () => {
            if (btnDetect) btnDetect.click();
        });
    }

    container.appendChild(group);
}

function createRecoverSection(container) {
    const group = document.createElement('div');
    group.className = 'form-group recover-group';

    const heading = document.createElement('h3');
    heading.textContent = 'Deadlock Recovery';
    group.appendChild(heading);

    const controlsRow = document.createElement('div');
    controlsRow.className = 'action-inputs-row';

    const policyLabel = document.createElement('label');
    policyLabel.textContent = 'Policy: ';
    policySelect = document.createElement('select');
    policySelect.id = 'recover-policy-select';

    const policies = [
        { label: 'Recovery cost', value: 'RECOVERY_COST' },
        { label: 'Priority', value: 'PRIORITY' },
        { label: 'Resources held', value: 'RESOURCES_HELD' }
    ];
    for (const p of policies) {
        const opt = document.createElement('option');
        opt.value = p.value;
        opt.textContent = p.label;
        policySelect.appendChild(opt);
    }
    policyLabel.appendChild(policySelect);
    controlsRow.appendChild(policyLabel);

    const strategyLabel = document.createElement('label');
    strategyLabel.textContent = ' Strategy: ';
    strategySelect = document.createElement('select');
    strategySelect.id = 'recover-strategy-select';

    const strategies = [
        { label: 'Terminate', value: 'TERMINATE' },
        { label: 'Preempt', value: 'PREEMPT' }
    ];
    for (const s of strategies) {
        const opt = document.createElement('option');
        opt.value = s.value;
        opt.textContent = s.label;
        strategySelect.appendChild(opt);
    }
    strategyLabel.appendChild(strategySelect);
    controlsRow.appendChild(strategyLabel);

    group.appendChild(controlsRow);

    const btnRow = document.createElement('div');
    btnRow.className = 'action-buttons-row';

    btnRecover = document.createElement('button');
    btnRecover.type = 'button';
    btnRecover.id = 'btn-action-recover';
    btnRecover.textContent = 'Recover';
    btnRow.appendChild(btnRecover);
    group.appendChild(btnRow);

    recoverResultLine = document.createElement('div');
    recoverResultLine.id = 'recover-result-line';
    recoverResultLine.className = 'action-result-line';
    group.appendChild(recoverResultLine);

    recoverErrorLine = document.createElement('div');
    recoverErrorLine.id = 'recover-error-line';
    recoverErrorLine.className = 'error-line';
    group.appendChild(recoverErrorLine);

    btnRecover.addEventListener('click', async () => {
        recoverErrorLine.textContent = '';
        recoverResultLine.textContent = '';
        const policy = policySelect.value;
        const strategy = strategySelect.value;

        try {
            const data = await recover(policy, strategy);
            const roundsStr = (data.rounds || [])
                .map(r => `victim ${r.victim} (${r.strategy})`)
                .join(', ');
            const resolvedStr = data.resolved ? 'resolved' : 'unresolved';
            recoverResultLine.textContent = roundsStr ? `Rounds: ${roundsStr}; ${resolvedStr}` : `Deadlock ${resolvedStr}`;

            if (typeof refreshHandler === 'function') {
                await refreshHandler();
            }
        } catch (err) {
            const code = err.code || 'ERROR';
            const msg = err.message || 'Recover failed';
            recoverErrorLine.textContent = `${code}: ${msg}`;
        }
    });

    const topBtnRecover = document.getElementById('btn-recover');
    if (topBtnRecover && !topBtnRecover.dataset.wired) {
        topBtnRecover.dataset.wired = 'true';
        topBtnRecover.addEventListener('click', () => {
            if (btnRecover) btnRecover.click();
        });
    }

    container.appendChild(group);
}

function createResetSection(container) {
    const group = document.createElement('div');
    group.className = 'form-group reset-group';

    const heading = document.createElement('h3');
    heading.textContent = 'Reset';
    group.appendChild(heading);

    const btnRow = document.createElement('div');
    btnRow.className = 'action-buttons-row';

    btnReset = document.createElement('button');
    btnReset.type = 'button';
    btnReset.id = 'btn-action-reset';
    btnReset.textContent = 'Reset';
    btnRow.appendChild(btnReset);
    group.appendChild(btnRow);

    resetErrorLine = document.createElement('div');
    resetErrorLine.id = 'reset-error-line';
    resetErrorLine.className = 'error-line';
    group.appendChild(resetErrorLine);

    btnReset.addEventListener('click', async () => {
        resetErrorLine.textContent = '';
        try {
            await reset(false);
            if (detectResultLine) detectResultLine.textContent = '';
            if (recoverResultLine) recoverResultLine.textContent = '';
            if (typeof refreshHandler === 'function') {
                await refreshHandler();
            }
        } catch (err) {
            if (err.code === 'VALIDATION_ERROR') {
                const confirmed = window.confirm('Reset will clear the event history. Continue?');
                if (confirmed) {
                    try {
                        await reset(true);
                        if (detectResultLine) detectResultLine.textContent = '';
                        if (recoverResultLine) recoverResultLine.textContent = '';
                        if (typeof refreshHandler === 'function') {
                            await refreshHandler();
                        }
                    } catch (innerErr) {
                        const code = innerErr.code || 'ERROR';
                        const msg = innerErr.message || 'Reset failed';
                        resetErrorLine.textContent = `${code}: ${msg}`;
                    }
                }
            } else {
                const code = err.code || 'ERROR';
                const msg = err.message || 'Reset failed';
                resetErrorLine.textContent = `${code}: ${msg}`;
            }
        }
    });

    const topBtnReset = document.getElementById('btn-reset');
    if (topBtnReset && !topBtnReset.dataset.wired) {
        topBtnReset.dataset.wired = 'true';
        topBtnReset.addEventListener('click', () => {
            if (btnReset) btnReset.click();
        });
    }

    container.appendChild(group);
}

export function renderDetectRecoverPanel(state, onRefresh) {
    if (typeof onRefresh === 'function') {
        refreshHandler = onRefresh;
    }

    const panel = document.getElementById('detect-recover-panel');
    if (!panel) {
        return;
    }

    const container = panel.querySelector('.panel-content') || panel;

    if (!initialized) {
        container.textContent = '';
        createDetectSection(container);
        createRecoverSection(container);
        createResetSection(container);
        initialized = true;
    }
}
