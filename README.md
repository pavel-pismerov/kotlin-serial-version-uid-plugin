# kotlin-serial-version-uid-plugin
IntelliJ Idea plugin for generating serialVersionUID in Kotlin classes.
Plugin automatically creates class body and companion object if not exist.

### HowTo
1. Open Kotlin file with class in editor
2. Press Ctr + Shift + M

### Future plans
- Add check for the already existed property (ask for replace the value)
- Implement possibility to generate property for all classes in *.kt file
- Add support to configure plugin (search across module/project)
- Ask to create generate serialVersionUID after class creation/implementing Serializable interface

> Created just to simplify the life of my team and has a huge amount of bugs (probably will be fixed later).  