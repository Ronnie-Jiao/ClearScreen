const test = require('node:test');
const assert = require('node:assert/strict');
const core = require('../targets/safari/assets/ad-filter-core.js');

test('requires an ad-specific context for ambiguous skip controls', () => {
  assert.equal(core.actionForLabel('跳过', false, false), null);
  assert.equal(core.actionForLabel('跳过', true, false), 'skip');
  assert.equal(core.actionForLabel('Skip', true, false), 'skip');
});

test('accepts explicit skip-ad labels without borrowing page-wide ad text', () => {
  assert.equal(core.actionForLabel('跳过广告', false, false), 'skip');
  assert.equal(core.actionForLabel('Skip ad', false, false), 'skip');
});

test('requires ad context for generic close buttons and icons', () => {
  assert.equal(core.actionForLabel('关闭', false, false), null);
  assert.equal(core.actionForLabel('×', false, true), null);
  assert.equal(core.actionForLabel('关闭', true, false), 'close');
  assert.equal(core.actionForLabel('', true, true), 'close');
});

test('does not treat navigation, download, purchase, or ordinary dialog actions as ads', () => {
  assert.equal(core.actionForLabel('跳转到正文', true, false), null);
  assert.equal(core.actionForLabel('取消', true, false), null);
  assert.equal(core.actionForLabel('立即下载', true, false), null);
  assert.equal(core.actionForLabel('购买', true, false), null);
});

test('limits clickable controls to small, visible-sized targets', () => {
  assert.equal(core.isSafeControl('close', { width: 48, height: 48 }, 390), true);
  assert.equal(core.isSafeControl('close', { width: 160, height: 48 }, 390), false);
  assert.equal(core.isSafeControl('skip', { width: 130, height: 48 }, 390), true);
  assert.equal(core.isSafeControl('skip', { width: 390, height: 220 }, 390), false);
});

test('requires close controls to sit near the ad container edge', () => {
  const container = { left: 20, top: 100, right: 320, bottom: 500, width: 300, height: 400 };
  assert.equal(core.isNearContainerEdge({ left: 270, top: 112, right: 310, bottom: 152 }, container), true);
  assert.equal(core.isNearContainerEdge({ left: 130, top: 250, right: 170, bottom: 290 }, container), false);
});
