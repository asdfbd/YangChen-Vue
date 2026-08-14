import type {PluginOption} from 'vite';

import {EOL} from 'node:os';

/**
 * 用于注入版权信息
 * @returns
 */
async function viteLicensePlugin(
): Promise<PluginOption | undefined> {
  return {
    apply: 'build',
    enforce: 'post',
    generateBundle: {
      handler(_options, bundle) {
        const copyrightText = ``.trim();
        for (const [, fileContent] of Object.entries(bundle)) {
          if (fileContent.type === 'chunk' && fileContent.isEntry) {
            // 插入版权信息
            const content = fileContent.code;
            const updatedContent = `${copyrightText}${EOL}${content}`;
            // 更新bundle
            fileContent.code = updatedContent;
          }
        }
      },
      order: 'post',
    },
    name: 'vite:license',
  };
}

export {viteLicensePlugin};
