# Source this file, then call the app's functions from your laptop:
#   source scripts/af.sh
#   af createNote '{"title": "Groceries", "content": "Milk, eggs, coffee"}'
# With more than one device attached, set ANDROID_SERIAL first.
# Single quotes inside the JSON break the quoting, so keep test text simple.

PKG=dev.omerdurmaz.notes
SVC=dev.omerdurmaz.notes.appfunctions.BaseNotesAppFunctionService

# usage: af <functionName> '<json>' [--brief-yaml]
af() {
  adb shell "cmd app_function execute-app-function --package $PKG \
    --function '$SVC#$1' --parameters '$2' ${3:-}"
}
