import { DashboardWidgetWeb, AutomationRuleWeb } from '../types';

export interface EmbedHubConfigBackup {
  version: string;
  app: string;
  exportedAt: string;
  dashboard: {
    widgets: DashboardWidgetWeb[];
  };
  automation: {
    rules: AutomationRuleWeb[];
  };
}

/**
 * Serializes the current dashboard layout and automation scripts into a formatted JSON file
 * and triggers a browser file download.
 */
export function exportDashboardAndAutomation(
  widgets: DashboardWidgetWeb[],
  rules: AutomationRuleWeb[],
  filename: string = 'embedhub-dashboard-automation.json'
): void {
  const backup: EmbedHubConfigBackup = {
    version: '1.0.0',
    app: 'EmbedHub',
    exportedAt: new Date().toISOString(),
    dashboard: {
      widgets
    },
    automation: {
      rules
    }
  };

  const jsonString = JSON.stringify(backup, null, 2);
  const blob = new Blob([jsonString], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}

/**
 * Validates and extracts widgets and automation rules from a JSON string.
 */
export function parseConfigBackup(jsonText: string): {
  widgets?: DashboardWidgetWeb[];
  rules?: AutomationRuleWeb[];
  error?: string;
} {
  try {
    const parsed = JSON.parse(jsonText);

    let widgets: DashboardWidgetWeb[] | undefined = undefined;
    let rules: AutomationRuleWeb[] | undefined = undefined;

    // Format 1: Standard EmbedHubConfigBackup
    if (parsed.dashboard && Array.isArray(parsed.dashboard.widgets)) {
      widgets = parsed.dashboard.widgets;
    } else if (Array.isArray(parsed.widgets)) {
      widgets = parsed.widgets;
    } else if (Array.isArray(parsed) && parsed.length > 0 && 'dataKey' in parsed[0]) {
      widgets = parsed;
    }

    if (parsed.automation && Array.isArray(parsed.automation.rules)) {
      rules = parsed.automation.rules;
    } else if (Array.isArray(parsed.rules)) {
      rules = parsed.rules;
    } else if (Array.isArray(parsed) && parsed.length > 0 && 'actionType' in parsed[0]) {
      rules = parsed;
    }

    if (!widgets && !rules) {
      return {
        error: 'JSON file does not contain recognized dashboard widgets or automation rules.'
      };
    }

    return { widgets, rules };
  } catch (err: any) {
    return {
      error: `Invalid JSON format: ${err.message || 'Syntax error'}`
    };
  }
}

/**
 * Opens a native file picker to select a .json file and restores the layout.
 */
export function openJsonFilePicker(
  onSuccess: (widgets?: DashboardWidgetWeb[], rules?: AutomationRuleWeb[]) => void,
  onError: (errorMessage: string) => void
): void {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = '.json,application/json';

  input.onchange = (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const text = event.target?.result as string;
      const result = parseConfigBackup(text);
      if (result.error) {
        onError(result.error);
      } else {
        onSuccess(result.widgets, result.rules);
      }
    };
    reader.onerror = () => {
      onError('Failed to read selected JSON file.');
    };
    reader.readAsText(file);
  };

  input.click();
}
