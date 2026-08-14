# @vben/plugins

该目录用于存放项目中集成的第三方库及其相关插件。每个插件都包含了可重用的逻辑、配置和组件，方便在项目中进行统一管理和调用。

## 注意

所有的第三方插件都必须以 `subpath` 形式引入，例：

以 `tiptap` 为例，引入方式如下：

**packages.json**

```json
"exports": {
    "./tiptap": {
      "types": "./src/tiptap/index.ts",
      "default": "./src/tiptap/index.ts"
    }
  }
```

**使用方式**

```ts
import { VbenTiptapPreview } from '@vben/plugins/tiptap';
```

这样做的好处是，应用可以自行选择是否使用插件，而不会因为插件的引入及副作用而导致打包体积增大，只引入需要的插件即可。
