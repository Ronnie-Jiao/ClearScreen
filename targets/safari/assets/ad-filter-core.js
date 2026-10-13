(function attachClearScreenAdFilter(root, factory) {
  const api = factory();
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  root.ClearScreenAdFilterCore = api;
})(globalThis, function createClearScreenAdFilterCore() {
  const explicitSkip = /(?:跳过\s*(?:广告|广告片)|skip\s*(?:the\s*)?(?:ad|advert(?:isement)?))/i;
  const genericSkip = /(?:跳过|\bskip\b)/i;
  const explicitClose = /(?:关闭\s*(?:广告|广告弹窗|弹窗)|close\s*(?:the\s*)?(?:ad|advert(?:isement)?|popup)|dismiss\s*(?:the\s*)?ad)/i;
  const genericClose = /(?:关闭|\bclose\b|\bdismiss\b|^[×✕✖x]$)/i;
  const adMarker = /广告|赞助|推广|sponsored|advertisement|advertising|promoted|\bad[-_ ]?(?:slot|unit|content|banner)?\b/i;
  const skipNavigation = /(?:skip\s+to|skip\s+navigation|跳至|跳转|跳过到|跳过导航)/i;

  function normalize(value) {
    return String(value || '').replace(/\s+/g, ' ').trim().slice(0, 320);
  }

  function hasAdMarker(value) {
    return adMarker.test(normalize(value));
  }

  function actionForLabel(label, hasAdContext, hasCloseIconHint) {
    const text = normalize(label);
    if ((!text && !(hasAdContext && hasCloseIconHint)) || skipNavigation.test(text)) return null;
    if (explicitSkip.test(text)) return 'skip';
    if (explicitClose.test(text)) return 'close';
    if (!hasAdContext) return null;
    if (genericSkip.test(text)) return 'skip';
    if (genericClose.test(text) || hasCloseIconHint) return 'close';
    return null;
  }

  function isSafeControl(action, rect, viewportWidth) {
    if (!rect || rect.width < 18 || rect.height < 14) return false;
    if (action === 'close') return rect.width <= 76 && rect.height <= 76;
    const widthLimit = Math.min(Math.max(viewportWidth || 390, 320) * 0.78, 420);
    return action === 'skip' && rect.width <= widthLimit && rect.height <= 112;
  }

  function isNearContainerEdge(control, container) {
    if (!control || !container) return false;
    const edgeX = Math.max(52, container.width * 0.14);
    const edgeY = Math.max(52, container.height * 0.18);
    const nearHorizontal = Math.abs(control.left - container.left) <= edgeX ||
      Math.abs(control.right - container.right) <= edgeX;
    const nearVertical = Math.abs(control.top - container.top) <= edgeY ||
      Math.abs(control.bottom - container.bottom) <= edgeY;
    return nearHorizontal && nearVertical;
  }

  return { actionForLabel, hasAdMarker, isNearContainerEdge, isSafeControl, normalize };
});
