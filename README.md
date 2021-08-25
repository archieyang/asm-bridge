# ASM Bridge
[![](https://jitpack.io/v/archieyang/asm-bridge.svg)](https://jitpack.io/#archieyang/asm-bridge)

ASM Bridge attempts to make it easier to do bytecode manipulation and analysis with [ASM](https://asm.ow2.io/) and [Android transform API](https://developer.android.com/reference/tools/gradle-api/7.0/com/android/build/api/transform/Transform) .

## Usage
Add jitpack in your root build.gradle at the end of repositories:
``` groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Add the dependency of ASM Bridge: 
``` groovy
dependencies {
    classpath 'com.github.archieyang.asm-bridge:asm-bridge:0.0.6'
}
```
Create buildSrc directory in your root directory and add ASM library dependencies in buildSrc/build.gradle
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
Create your own ClassVisitor(with some magic) in buildSrc: 

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
Configure your custom ClassVisitor in build.gradle
``` groovy
asmBridge {
    implementation = "${your adapter package name}.CustomAdapter"
}
```

## Acknowledgments
The world famous Java bytecode manipulation and analysis framework: [ASM](https://asm.ow2.io/)

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