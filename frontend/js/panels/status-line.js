export function renderStatusLine(state) {
    const el = document.getElementById('status-line');
    if (!el) {
        return;
    }

    const actualState = (state && state.state) ? state.state : state;
    if (!actualState) {
        return;
    }

    const scenarioName = actualState.scenario ? actualState.scenario.name : '';
    const step = actualState.step !== undefined ? actualState.step : 0;
    const prev = actualState.prevention || {};

    const me = prev.mutualExclusion ? 'on' : 'off';
    const haw = prev.holdAndWait ? 'on' : 'off';
    const np = prev.noPreemption ? 'on' : 'off';
    const cw = prev.circularWait ? 'on' : 'off';

    el.textContent = `Scenario: ${scenarioName} | Step: ${step} | Prevention: mutualExclusion: ${me}, holdAndWait: ${haw}, noPreemption: ${np}, circularWait: ${cw}`;
}
