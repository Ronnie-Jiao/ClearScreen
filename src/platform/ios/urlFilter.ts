export const IOS_URL_FILTER_MINIMUM_MAJOR_VERSION = 26;

export type IOSURLFilterReadiness = 'unsupported' | 'integration-required';

export function getIOSURLFilterReadiness(version: string | number): IOSURLFilterReadiness {
  const majorVersion = Number.parseInt(String(version).split('.')[0], 10);
  return Number.isFinite(majorVersion) && majorVersion >= IOS_URL_FILTER_MINIMUM_MAJOR_VERSION
    ? 'integration-required'
    : 'unsupported';
}

export function getIOSURLFilterReadinessLabel(readiness: IOSURLFilterReadiness): string {
  return readiness === 'integration-required' ? '尚未接入' : '需要 iOS 26';
}
