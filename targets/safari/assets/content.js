(function startClearScreenSafariFilter() {
  if (window.top !== window || !globalThis.ClearScreenAdFilterCore || !globalThis.browser?.storage?.local) return;

  const core = globalThis.ClearScreenAdFilterCore;
  const selector = 'button, a[href], [role="button"], input[type="button"], input[type="submit"]';
  const handledControls = new WeakSet();
  const handledAdContainers = new WeakSet();
  let enabled = false;
  let observer = null;
  let scanTimer = null;

  function classText(element) {
    const value = element.className;
    return typeof value === 'string' ? value : String(value?.baseVal || '');
  }

  function getLabel(element) {
    return [
      element.getAttribute('aria-label'),
      element.getAttribute('title'),
      element.value,
      element.innerText,
      element.textContent,
    ].filter(Boolean).join(' ').replace(/\s+/g, ' ').trim().slice(0, 320);
  }

  function getAdContainer(element) {
    let textContainer = null;
    let current = element.parentElement;
    const viewArea = Math.max(1, window.innerWidth * window.innerHeight);

    for (let depth = 0; current && depth < 8; depth += 1, current = current.parentElement) {
      if (current === document.body || current === document.documentElement) break;
      const metadata = [
        current.id,
        classText(current),
        current.getAttribute('aria-label'),
        current.getAttribute('title'),
        current.getAttribute('data-testid'),
        current.getAttribute('data-ad'),
        current.getAttribute('data-advertisement'),
      ].filter(Boolean).join(' ');
      if (core.hasAdMarker(metadata)) return current;

      const text = String(current.innerText || current.textContent || '').replace(/\s+/g, ' ').trim();
      if (!textContainer && text.length <= 1600 && core.hasAdMarker(text)) {
        const rect = current.getBoundingClientRect();
        if (rect.width * rect.height <= viewArea * 0.82) textContainer = current;
      }
    }
    return textContainer;
  }

  function visibleAndEnabled(element) {
    if (element.disabled || element.getAttribute('aria-disabled') === 'true' || element.closest('[aria-hidden="true"]')) return false;
    const rect = element.getBoundingClientRect();
    const style = window.getComputedStyle(element);
    return rect.width > 0 && rect.height > 0 && style.display !== 'none' &&
      style.visibility !== 'hidden' && Number(style.opacity || 1) > 0.05;
  }

  function maybeClick(element) {
    if (handledControls.has(element) || !visibleAndEnabled(element)) return;
    const label = getLabel(element);
    const container = getAdContainer(element);
    if (container && handledAdContainers.has(container)) return;
    const metadata = `${label} ${classText(element)} ${element.id || ''}`;
    const iconHint = /(?:close|dismiss|关闭)[-_ ]?(?:icon|button|btn)?/i.test(metadata);
    const action = core.actionForLabel(label, Boolean(container), iconHint);
    if (!action) return;

    const rect = element.getBoundingClientRect();
    const safeRect = { width: rect.width, height: rect.height };
    if (!core.isSafeControl(action, safeRect, window.innerWidth)) return;
    if (action === 'close' && container) {
      const outer = container.getBoundingClientRect();
      if (!core.isNearContainerEdge(rect, outer)) return;
    }
    if (action === 'close' && !container && !/(?:关闭广告|关闭弹窗|close\s+(?:the\s+)?(?:ad|popup)|dismiss\s+(?:the\s+)?ad)/i.test(label)) return;

    handledControls.add(element);
    if (container) handledAdContainers.add(container);
    element.click();
  }

  function scan() {
    scanTimer = null;
    if (!enabled || !document.body) return;
    const controls = document.querySelectorAll(selector);
    const limit = Math.min(controls.length, 350);
    for (let index = 0; index < limit; index += 1) maybeClick(controls[index]);
  }

  function scheduleScan() {
    if (!enabled || scanTimer !== null) return;
    scanTimer = window.setTimeout(scan, 350);
  }

  function setEnabled(value) {
    if (enabled === value) return;
    enabled = value;
    if (scanTimer !== null) window.clearTimeout(scanTimer);
    scanTimer = null;
    if (!enabled) {
      observer?.disconnect();
      observer = null;
      return;
    }
    scan();
    observer = new MutationObserver(scheduleScan);
    observer.observe(document.body, {
      subtree: true,
      childList: true,
      characterData: true,
      attributes: true,
      attributeFilter: ['aria-label', 'class', 'data-ad', 'data-advertisement', 'disabled', 'hidden', 'title'],
    });
  }

  browser.storage.local.get('enabled').then((settings) => setEnabled(settings.enabled === true)).catch(() => setEnabled(false));
  browser.storage.onChanged.addListener((changes, areaName) => {
    if (areaName === 'local' && changes.enabled) setEnabled(changes.enabled.newValue === true);
  });
})();
