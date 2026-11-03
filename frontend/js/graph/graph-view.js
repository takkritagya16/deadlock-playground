let cy = null;

if (typeof cytoscape !== 'undefined' && typeof cytoscapeDagre !== 'undefined') {
    cytoscape.use(cytoscapeDagre);
}

export function getCy() {
    return cy;
}

export function renderGraph(state) {
    const container = document.getElementById('graph');
    if (!container) {
        return;
    }

    if (!cy) {
        if (typeof cytoscape !== 'undefined' && typeof cytoscapeDagre !== 'undefined') {
            cytoscape.use(cytoscapeDagre);
        }
        const placeholder = container.querySelector('.placeholder-text');
        if (placeholder) {
            placeholder.remove();
        }
        cy = cytoscape({
            container,
            elements: []
        });
        if (typeof window !== 'undefined') {
            window.cy = cy;
        }
    }

    cy.elements().remove();

    const actualState = (state && state.state) ? state.state : state;
    const graph = actualState && actualState.graph;
    const nodes = graph && Array.isArray(graph.nodes) ? graph.nodes : [];
    const edges = graph && Array.isArray(graph.edges) ? graph.edges : [];

    if (!graph || nodes.length === 0) {
        return;
    }

    cy.add([...nodes, ...edges]);

    cy.layout({
        name: 'dagre',
        rankDir: 'LR'
    }).run();
}
