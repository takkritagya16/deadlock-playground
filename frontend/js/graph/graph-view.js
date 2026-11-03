let cy = null;
let currentProcesses = [];

if (typeof cytoscape !== 'undefined' && typeof cytoscapeDagre !== 'undefined') {
    cytoscape.use(cytoscapeDagre);
}

function getEdgeCount(ele) {
    const rawCount = ele.data('count');
    if (typeof rawCount === 'number') {
        return rawCount;
    }
    const source = ele.data('source');
    const target = ele.data('target');
    const kind = ele.data('kind');

    if (kind === 'request') {
        const proc = currentProcesses.find(p => p.id === source);
        if (proc && proc.waitingFor && proc.waitingFor.resource === target) {
            return proc.waitingFor.count;
        }
    } else if (kind === 'allocation') {
        const proc = currentProcesses.find(p => p.id === target);
        if (proc && Array.isArray(proc.held)) {
            const entry = proc.held.find(h => h.resource === source);
            if (entry) {
                return entry.count;
            }
        }
    }
    return 1;
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
                },
                {
                    selector: 'edge',
                    style: {
                        'curve-style': 'bezier',
                        'width': 1.5,
                        'line-color': '#000000',
                        'target-arrow-shape': 'triangle',
                        'target-arrow-color': '#000000',
                        'font-family': 'Georgia, serif',
                        'font-size': '10px',
                        'color': '#000000',
                        'text-background-color': '#ffffff',
                        'text-background-opacity': 1,
                        'text-background-padding': '2px',
                        'label': function(ele) {
                            const count = getEdgeCount(ele);
                            return count > 1 ? String(count) : '';
                        }
                    }
                },
                {
                    selector: 'edge[kind = "allocation"]',
                    style: {
                        'line-style': 'solid'
                    }
                },
                {
                    selector: 'edge[kind = "request"]',
                    style: {
                        'line-style': 'dashed'
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
    currentProcesses = actualState && Array.isArray(actualState.processes) ? actualState.processes : [];

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
