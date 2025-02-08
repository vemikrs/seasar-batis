# SeasarBatis

## MyBatis Generator との統合

### プラグインの設定
`generatorConfig.xml`に以下のプラグイン設定を追加してください：

```xml
<plugin type="jp.vemi.seasarbatis.generator.SBEntityMetaPlugin">
    <property name="addSchemaName" value="true"/>
</plugin>
```

### サンプル設定
完全な設定例は`src/test/resources/sample-generatorConfig.xml`を参照してください。

