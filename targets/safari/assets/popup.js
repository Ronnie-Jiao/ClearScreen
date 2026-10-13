(function initializeClearScreenSafariPopup() {
  const toggle = document.getElementById('enabled');
  const status = document.getElementById('status');

  function render(enabled) {
    toggle.checked = enabled;
    status.textContent = enabled ? '已开启' : '已关闭';
  }

  browser.storage.local.get('enabled')
    .then((settings) => render(settings.enabled === true))
    .catch(() => {
      toggle.disabled = true;
      status.textContent = '扩展暂不可用';
    });

  toggle.addEventListener('change', () => {
    const enabled = toggle.checked;
    status.textContent = enabled ? '正在开启…' : '正在关闭…';
    browser.storage.local.set({ enabled })
      .then(() => render(enabled))
      .catch(() => {
        toggle.checked = !enabled;
        status.textContent = '保存失败，请重试';
      });
  });
})();
