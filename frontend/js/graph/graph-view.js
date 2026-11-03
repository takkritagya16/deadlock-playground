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
            elements: [],
            style: [
                {
                    selector: 'node',
                    style: {
                        'font-family': 'Georgia, serif',
                        'color': '#000000',
                        'text-valign': 'center',
                        'text-halign': 'center',
                        'padding': '8px',
                        'width': 'label',
                        'height': 'label'
                    }
                },
                {
                    selector: 'node[type = "process"]',
                    style: {
                        'shape': 'ellipse',
                        'background-color': '#ffffff',
                        'border-width': 1,
                        'border-color': '#000000',
                        'label': 'data(id)'
                    }
                },
                {
                    selector: 'node[type = "resource"]',
                    style: {
                        'shape': 'rectangle',
                        'background-color': '#ffffff',
                        'border-width': 1,
                        'border-color': '#000000',
                        'label': function(ele) {
                            const id = ele.data('id');
                            const instances = ele.data('instances');
                            if (!instances || instances <= 0) {
                                return id;
                            }
                            const dots = Array(instances).fill('\u25CF').join(' ');
                            return `${id} ${dots}`;
                        }
                    }
                }
            ]
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
