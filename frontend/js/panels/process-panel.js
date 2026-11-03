export function renderProcessPanel(state) {
    const panel = document.getElementById('process-panel');
    if (!panel) {
        return;
    }

    const container = panel.querySelector('.panel-content') || panel;
    container.textContent = '';

    const processes = state && Array.isArray(state.processes) ? state.processes : [];
    if (processes.length === 0) {
        const emptyMsg = document.createElement('p');
        emptyMsg.className = 'placeholder-text';
        emptyMsg.textContent = 'No processes';
        container.appendChild(emptyMsg);
        return;
    }

    for (const process of processes) {
        const item = document.createElement('div');

        const line1 = document.createElement('div');
        const info = document.createElement('span');
        info.textContent = `${process.id} (Priority: ${process.priority}, State: ${process.state})`;
        line1.appendChild(info);

        if (process.deadlocked) {
            const space = document.createTextNode(' ');
            line1.appendChild(space);
            const badge = document.createElement('span');
            badge.className = 'deadlocked';
            badge.textContent = 'DEADLOCKED';
            line1.appendChild(badge);
        }
        item.appendChild(line1);

        const heldDiv = document.createElement('div');
        if (process.held && process.held.length > 0) {
            const heldEntries = process.held.map(h => `${h.resource} x ${h.count}`).join(', ');
            heldDiv.textContent = `Held: ${heldEntries}`;
        } else {
            heldDiv.textContent = 'Held: none';
        }
        item.appendChild(heldDiv);

        if (process.waitingFor) {
            const waitingDiv = document.createElement('div');
            waitingDiv.textContent = `waiting for ${process.waitingFor.resource} x ${process.waitingFor.count}`;
            item.appendChild(waitingDiv);
        }

        const pendingDiv = document.createElement('div');
        pendingDiv.textContent = `Pending operations: ${process.pendingOperations}`;
        item.appendChild(pendingDiv);

        container.appendChild(item);
    }
}
