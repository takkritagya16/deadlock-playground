export function renderResourcePanel(state) {
    const panel = document.getElementById('resource-panel');
    if (!panel) {
        return;
    }

    const container = panel.querySelector('.panel-content') || panel;
    container.textContent = '';

    const resources = state && Array.isArray(state.resources) ? state.resources : [];
    if (resources.length === 0) {
        const emptyMsg = document.createElement('p');
        emptyMsg.className = 'placeholder-text';
        emptyMsg.textContent = 'No resources';
        container.appendChild(emptyMsg);
        return;
    }

    for (const resource of resources) {
        const item = document.createElement('div');
        const shareableStr = resource.shareable ? 'yes' : 'no';
        item.textContent = `${resource.id} (Total: ${resource.total}, Available: ${resource.available}, Shareable: ${shareableStr})`;
        container.appendChild(item);
    }
}
