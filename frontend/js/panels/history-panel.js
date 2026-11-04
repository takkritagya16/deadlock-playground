export function renderHistoryPanel(events, currentSeq, onJump) {
    const panel = document.getElementById('history-panel');
    if (!panel) {
        return;
    }

    const container = panel.querySelector('.panel-content') || panel;
    container.textContent = '';

    let eventList = [];
    if (Array.isArray(events)) {
        eventList = events;
    } else if (events && Array.isArray(events.events)) {
        eventList = events.events;
    }

    if (eventList.length === 0) {
        const emptyMsg = document.createElement('p');
        emptyMsg.className = 'placeholder-text';
        emptyMsg.textContent = 'No events recorded yet.';
        container.appendChild(emptyMsg);
        return;
    }

    for (const ev of eventList) {
        const row = document.createElement('div');
        row.className = 'history-row';
        row.dataset.seq = String(ev.seq);
        row.setAttribute('tabindex', '0');
        row.setAttribute('role', 'button');

        if (currentSeq !== undefined && currentSeq !== null && ev.seq === currentSeq) {
            row.classList.add('current-event');
        }

        const seqSpan = document.createElement('span');
        seqSpan.className = 'history-seq';
        seqSpan.textContent = String(ev.seq);

        const typeSpan = document.createElement('span');
        typeSpan.className = 'history-type';
        typeSpan.textContent = ev.type || '';

        const descSpan = document.createElement('span');
        descSpan.className = 'history-desc';
        descSpan.textContent = ev.description || '';

        row.appendChild(seqSpan);
        row.appendChild(document.createTextNode(' '));
        row.appendChild(typeSpan);
        row.appendChild(document.createTextNode(': '));
        row.appendChild(descSpan);

        if (typeof onJump === 'function') {
            row.style.cursor = 'pointer';
            row.addEventListener('click', () => {
                onJump(ev.seq);
            });
            row.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    onJump(ev.seq);
                }
            });
        }

        container.appendChild(row);
    }
}
