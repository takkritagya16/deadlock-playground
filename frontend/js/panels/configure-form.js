export function renderConfigureForm(container, onConfigured) {
    if (!container) {
        return;
    }

    const target = container.querySelector('.panel-content') || container;
    target.textContent = '';

    const form = document.createElement('form');
    form.id = 'form-configure';

    // Scenario Name
    const nameGroup = document.createElement('div');
    nameGroup.className = 'form-group';
    const nameLabel = document.createElement('label');
    nameLabel.textContent = 'Scenario Name: ';
    const nameInput = document.createElement('input');
    nameInput.type = 'text';
    nameInput.id = 'cfg-name';
    nameLabel.appendChild(nameInput);
    nameGroup.appendChild(nameLabel);
    form.appendChild(nameGroup);

    // Resources
    const resGroup = document.createElement('div');
    resGroup.className = 'form-group';
    const resHeading = document.createElement('h3');
    resHeading.textContent = 'Resources';
    resGroup.appendChild(resHeading);

    const resList = document.createElement('div');
    resList.className = 'resource-list';
    resGroup.appendChild(resList);

    function addResourceRow(defaultId = '', defaultTotal = 1, defaultShareable = false) {
        const row = document.createElement('div');
        row.className = 'resource-row';

        const idLabel = document.createElement('label');
        idLabel.textContent = 'ID: ';
        const idInput = document.createElement('input');
        idInput.type = 'text';
        idInput.className = 'cfg-res-id';
        idInput.value = defaultId;
        idLabel.appendChild(idInput);
        row.appendChild(idLabel);

        const totalLabel = document.createElement('label');
        totalLabel.textContent = ' Total: ';
        const totalInput = document.createElement('input');
        totalInput.type = 'number';
        totalInput.min = '1';
        totalInput.className = 'cfg-res-total';
        totalInput.value = defaultTotal;
        totalLabel.appendChild(totalInput);
        row.appendChild(totalLabel);

        const shareLabel = document.createElement('label');
        shareLabel.textContent = ' Shareable: ';
        const shareInput = document.createElement('input');
        shareInput.type = 'checkbox';
        shareInput.className = 'cfg-res-shareable';
        shareInput.checked = defaultShareable;
        shareLabel.appendChild(shareInput);
        row.appendChild(shareLabel);

        resList.appendChild(row);
    }

    addResourceRow('R1', 1, false);

    const addResBtn = document.createElement('button');
    addResBtn.type = 'button';
    addResBtn.textContent = 'Add resource';
    addResBtn.addEventListener('click', () => {
        const count = resList.children.length + 1;
        addResourceRow('R' + count, 1, false);
    });
    resGroup.appendChild(addResBtn);
    form.appendChild(resGroup);

    // Processes
    const procGroup = document.createElement('div');
    procGroup.className = 'form-group';
    const procHeading = document.createElement('h3');
    procHeading.textContent = 'Processes';
    procGroup.appendChild(procHeading);

    const procList = document.createElement('div');
    procList.className = 'process-list-inputs';
    procGroup.appendChild(procList);

    function addProcessRow(defaultId = '', defaultPriority = 1) {
        const row = document.createElement('div');
        row.className = 'process-row';

        const idLabel = document.createElement('label');
        idLabel.textContent = 'ID: ';
        const idInput = document.createElement('input');
        idInput.type = 'text';
        idInput.className = 'cfg-proc-id';
        idInput.value = defaultId;
        idLabel.appendChild(idInput);
        row.appendChild(idLabel);

        const prioLabel = document.createElement('label');
        prioLabel.textContent = ' Priority: ';
        const prioInput = document.createElement('input');
        prioInput.type = 'number';
        prioInput.className = 'cfg-proc-priority';
        prioInput.value = defaultPriority;
        prioLabel.appendChild(prioInput);
        row.appendChild(prioLabel);

        procList.appendChild(row);
    }

    addProcessRow('P1', 1);

    const addProcBtn = document.createElement('button');
    addProcBtn.type = 'button';
    addProcBtn.textContent = 'Add process';
    addProcBtn.addEventListener('click', () => {
        const count = procList.children.length + 1;
        addProcessRow('P' + count, 1);
    });
    procGroup.appendChild(addProcBtn);
    form.appendChild(procGroup);

    // Operations
    const opsGroup = document.createElement('div');
    opsGroup.className = 'form-group';
    const opsHeading = document.createElement('h3');
    opsHeading.textContent = 'Operations';
    opsGroup.appendChild(opsHeading);

    const opsTextarea = document.createElement('textarea');
    opsTextarea.id = 'cfg-operations';
    opsTextarea.rows = 5;
    opsTextarea.placeholder = 'P1 REQUEST R1 1\nP1 RELEASE R1 1';
    opsGroup.appendChild(opsTextarea);
    form.appendChild(opsGroup);

    // Prevention toggles
    const prevGroup = document.createElement('div');
    prevGroup.className = 'form-group prevention-toggles';
    const prevHeading = document.createElement('h3');
    prevHeading.textContent = 'Prevention';
    prevGroup.appendChild(prevHeading);

    const toggles = [
        { label: 'Mutual exclusion', key: 'mutualExclusion' },
        { label: 'Hold and wait', key: 'holdAndWait' },
        { label: 'No preemption', key: 'noPreemption' },
        { label: 'Circular wait', key: 'circularWait' }
    ];

    const toggleInputs = {};
    for (const t of toggles) {
        const label = document.createElement('label');
        const chk = document.createElement('input');
        chk.type = 'checkbox';
        chk.id = 'cfg-prev-' + t.key;
        toggleInputs[t.key] = chk;
        label.appendChild(chk);
        const text = document.createTextNode(' ' + t.label + ' ');
        label.appendChild(text);
        prevGroup.appendChild(label);
    }
    form.appendChild(prevGroup);

    // Submit button & error line
    const actionGroup = document.createElement('div');
    actionGroup.className = 'form-group';
    const submitBtn = document.createElement('button');
    submitBtn.type = 'submit';
    submitBtn.id = 'btn-submit-configure';
    submitBtn.textContent = 'Configure';
    actionGroup.appendChild(submitBtn);

    const errorLine = document.createElement('div');
    errorLine.id = 'cfg-error-line';
    errorLine.className = 'error-line';
    actionGroup.appendChild(errorLine);
    form.appendChild(actionGroup);

    function parseForm() {
        const name = nameInput.value.trim();
        if (!name) {
            return { error: 'Scenario name must not be blank.' };
        }

        const resources = [];
        const resourceIds = new Set();
        const resRows = resList.querySelectorAll('.resource-row');
        if (resRows.length === 0) {
            return { error: 'At least one resource is required.' };
        }
        for (let i = 0; i < resRows.length; i++) {
            const row = resRows[i];
            const idInput = row.querySelector('.cfg-res-id');
            const totalInput = row.querySelector('.cfg-res-total');
            const shareInput = row.querySelector('.cfg-res-shareable');

            const id = idInput ? idInput.value.trim() : '';
            if (!id) {
                return { error: 'Resource row ' + (i + 1) + ': ID must not be blank.' };
            }
            if (resourceIds.has(id)) {
                return { error: 'Resource ID must be unique: ' + id };
            }
            resourceIds.add(id);

            const total = totalInput ? parseInt(totalInput.value, 10) : 1;
            if (isNaN(total) || total < 1) {
                return { error: 'Resource ' + id + ': total must be a whole number of at least 1.' };
            }

            resources.push({
                id,
                total,
                shareable: shareInput ? shareInput.checked : false
            });
        }

        const processes = [];
        const processIds = new Set();
        const procRows = procList.querySelectorAll('.process-row');
        if (procRows.length === 0) {
            return { error: 'At least one process is required.' };
        }
        for (let i = 0; i < procRows.length; i++) {
            const row = procRows[i];
            const idInput = row.querySelector('.cfg-proc-id');
            const prioInput = row.querySelector('.cfg-proc-priority');

            const id = idInput ? idInput.value.trim() : '';
            if (!id) {
                return { error: 'Process row ' + (i + 1) + ': ID must not be blank.' };
            }
            if (processIds.has(id)) {
                return { error: 'Process ID must be unique: ' + id };
            }
            processIds.add(id);

            const priority = prioInput && prioInput.value.trim() !== '' ? parseInt(prioInput.value, 10) : 1;
            if (isNaN(priority)) {
                return { error: 'Process ' + id + ': priority must be a valid number.' };
            }

            processes.push({
                id,
                priority,
                operations: []
            });
        }

        const rawOps = opsTextarea.value.split('\n');
        let lineIndex = 0;
        for (const rawLine of rawOps) {
            lineIndex++;
            const line = rawLine.trim();
            if (!line) {
                continue;
            }
            const words = line.split(/\s+/);
            if (words.length !== 4) {
                return { error: 'Operation line ' + lineIndex + ': must have exactly four words.' };
            }
            const [procId, opTypeRaw, resId, countStr] = words;
            if (!processIds.has(procId)) {
                return { error: 'Operation line ' + lineIndex + ': process ' + procId + ' does not exist.' };
            }
            const opType = opTypeRaw.toUpperCase();
            if (opType !== 'REQUEST' && opType !== 'RELEASE') {
                return { error: 'Operation line ' + lineIndex + ': type must be REQUEST or RELEASE.' };
            }
            if (!resourceIds.has(resId)) {
                return { error: 'Operation line ' + lineIndex + ': resource ' + resId + ' does not exist.' };
            }
            const count = Number(countStr);
            if (!Number.isInteger(count) || count < 1) {
                return { error: 'Operation line ' + lineIndex + ': count must be a whole number of at least 1.' };
            }

            const proc = processes.find(p => p.id === procId);
            if (proc) {
                proc.operations.push({
                    type: opType,
                    resource: resId,
                    count: count
                });
            }
        }

        const prevention = {
            mutualExclusion: toggleInputs.mutualExclusion.checked,
            holdAndWait: toggleInputs.holdAndWait.checked,
            noPreemption: toggleInputs.noPreemption.checked,
            circularWait: toggleInputs.circularWait.checked
        };

        return {
            body: {
                name,
                prevention,
                resources,
                processes
            }
        };
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        errorLine.textContent = '';
        const parsed = parseForm();
        if (parsed.error) {
            errorLine.textContent = parsed.error;
            return;
        }

        if (typeof onConfigured === 'function') {
            onConfigured(parsed.body);
        }
    });

    target.appendChild(form);
}
