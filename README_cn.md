# ASM Bridge
[![](https://jitpack.io/v/archieyang/asm-bridge.svg)](https://jitpack.io/#archieyang/asm-bridge)

ASM Bridge通过gradle插件的方式简化了使用 [ASM](https://asm.ow2.io/) 和[Android transform API](https://developer.android.com/reference/tools/gradle-api/7.0/com/android/build/api/transform/Transform) 进行字节码修改和分析的开发过程。

## 使用方式
在根目录下的build.gradle添加jitpack：
``` groovy
buildscript {
    ...
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
在根目录下的build.gradle添加ASM Bridge依赖
``` groovy
buildscript {
    ...
    dependencies {
        classpath 'com.github.archieyang.asm-bridge:asm-bridge:0.0.6'
    }
}
```
在根目录下创建buildSrc并在buildSrc/build.gradle中添加ASM依赖：
``` groovy
dependencies {
    implementation "org.ow2.asm:asm:7.2"
    implementation "org.ow2.asm:asm-commons:7.2"
    implementation "org.ow2.asm:asm-analysis:7.2"
    implementation "org.ow2.asm:asm-util:7.1"
    implementation "org.ow2.asm:asm-tree:7.2"
    implementation "com.android.tools.build:gradle:3.4.1"
}
```
在buildSrc创建你自己的ClassVisitor： 

```java
public class CustomAdapter extends ClassVisitor {
    public CustomAdapter(final ClassVisitor classVisitor) {
        super(Opcodes.ASM5, classVisitor);
    }

    @Override
    public void visit(final int i, final int i1, final String s, final String s1, final String s2, final String[] strings) {
        super.visit(i, i1, s, s1, s2, strings);

        if (s != null && (s.endsWith("Activity"))) {
            Logger.log("Class name ends with Activity: " + s);
        }
    }
}
```
在你应用的App模块下的build.gradle中配置ClassVisitor的实现：
``` groovy
apply plugin: 'com.github.archieyang.asm-bridge'
asmBridge {
    implementation = "${your adapter package name}.CustomAdapter"
}
```

完整的示例在 [这里](https://github.com/archieyang/asm-bridge-sample).
## 致谢
[ASM](https://asm.ow2.io/)

## License
    Copyright 2021 Archie Yang

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.