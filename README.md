# Phial [![Release](https://jitpack.io/v/roshakorost/Phial.svg)](https://jitpack.io/#roshakorost/Phial)  [![Build Status](https://travis-ci.org/roshakorost/Phial.svg?branch=master)](https://travis-ci.org/roshakorost/Phial)

**Phial is an Android library that captures your bug data directly to Jira or other services.** When a bug is found, Phial allows the Tester to export logs, screenshot, system, build and many more data from a device to a JIRA bug report. 

Automated bug capture on demand increases the amount of time Developers can spend building new features by ensuring they have all the relevant data needed to reproduce a bug.  This also reduces the time QA spends documenting bugs.


**Feature summary** 
- Phial captures all necessary information about the bug: build version, commit, extended device information, screenshot, logs.
- Developers can easily extend Phial to add additional data types, so you can include your SQLite database or SharedPreferences as well.
- Provides debug view to monitor your app state.
- Ability to create your own debug views, that will be in debug app.
- Easy export of bug Data to Jira and other options. *The more recent the snapshot the more valuable the data is.*


You can easily include Phial into `internal`/`debug` builds, without adding to `release` build.

### Screenshots

![DemoScreenshot][1]

### [Example of attachment][2]

## Share
By default Phial’s share menu only shows installed applications that can handle zip attachments. However you might want to include your own share options e.g. creating Jira Issue or posting to a specific slack channel.

### Phial-Jira
Phial-jira allows you to login to your Jira and create an issue with debug data attached to it.
Login page will be shown only the first time. After that the saved credentials will be used.
```java
final Shareable jiraShareable = new JiraShareableBuilder(app)
    .setBaseUrl(url) //Jira url
    .setProjectKey(projectKey)	//project key
    .build();

PhialOverlay.builder(app)
    .addShareable(jiraShareable)
    .initPhial();
```
**Note:** since credentials are not stored securely  it’s recommended to use Phial only in internal/debug builds.

### Shareable
You can add your own share options by implementing `Shareable`.
When user selects your share option `void share(ShareContext shareContext, File zippedAttachment, String message);` will be called.
You should call either `shareContext.onSuccess()`, `shareContext.onFail(message)` or `shareContext.onCancel()` when share is finished.
`ShareContext` also provides interface for adding your UI elements in case you need authorization or some extra fields. See `JiraShareable` from `phial-jira` as an example implementation.

# Enhance Your Attachments
## Custom Keys
Phial allows you to associate arbitrary key/value pairs which are viewable right from the Debug Overlay. These key values pairs are included as JSON with the attachment to be shared.

Setting key is done by calling `Phial.setKey(key, value)`. 

You can provide a category to group your data via  `Phial.category(name)`.

Re-setting the same key will update the value. 

**Examples:**
```
Phial.setKey("currentUser", "admin");
Phial.setKey("currentUser", "user1");
```
This will include key/value **"currentUser" "user1"** in share attachment.

**Note:** Keys that are associated with different categories are treated as different keys.

**Note:** Setting `null` will not remove an association, but will set null value to the key. In order to remove the association use `Phial.removeKey(key)` or `Phial.category(name).removeKey(key)`

If you use `Crashlytics` you might want to integrate Phial key/value with `Crashlytics` as well.
You can do that by implementing `Saver` interface and adding it using `Phial.addSaver(saver)`


## Logging
You can include your application logs into share attachment. If you don’t save your log on disk you can use `phial-logging` in order to attach html-formatted logs with your debug data.

#### Integration
1. Add phial-logger to your dependencies.
2. Add `PhialLogger` to the attachers:
```java
final PhialLogger phialLogger = new PhialLogger(app);
PhialOverlay.builder(app)
    .addAttachmentProvider(phialLogger)
    .initPhial();
```
`phialLogger.log(priority, tag, message,throwable)` will dump the log to the file that will be included into share attachment.

**Note:** it is recommended to use some kind of a logging facade instead of calling `phialLogger.log(priority, tag, message,throwable)` manually. Check [Timber Integration Example.][3]

If you already store your logs on the device you can use attachers in order to add them to the share attachment. See the next section on how to implement custom attachers.


## Custom attachers

Phial allows to include your custom data into share attachments. For example, you might want to include SQLite database, logs or SharedPreferences files  from the device in order to investigate an issue.

If you need to include a file to share with the attachment you can use `SimpleFileAttacher(File file)` or `SimpleFileAttacher(Collection<File> files).
If the file is a directory all files from it will be attached as well.

Example:
```java
PhialOverlay.builder(app)
    .addAttachmentProvider(new SimpleFileAttacher(sqlLiteFile))
    .initPhial();
```
In case you want to include some information that is not persisted to file, you can use 
`Attacher` or `ListAttacher`.

Currently Attacher API works only with files, so when `provideAttachment` is called you should dump the data to a temporary file and return it. When `onAttachmentNotNeeded` is called the temporary file can be deleted (see SharedPreferencesAttacher in the sample app or KVAttacher for an example).

## Automatic fields filling
Phial-autofill allows you to fill `EditTexts` and `TextViews` with predefined data.

![DemoScreenshot][3]

Example of adding your options:
```java
final Page autoFillPage = createPhialPage(
        forActivity(AutoFillActivity.class)      //for what activity AutoFill page should be enabled.
                .fill(R.id.login, R.id.password) //what fields should be filled.
                .withOptions(
                        option("user Q" /*name of option*/, "QQQQQQ"/*first field to fill*/, "Qpwdpwd1"/*second field*/),
                        option("user W", "WWWWWW", leaveEmpty()),
                        option("user E", "EEEEEE", "Epwdpwd3"),
                        option("user R", "RRRRRR", "Rpwdpwd4")
                )
);
```
You can also save the data directly from the application. Fill specified fields, enter a name and press `ADD` button.

## Custom overlay pages
You can add custom pages that will be available in the overlay.
To do this provide your instance of `Page` class to the `PhialOverlay.Builder`.

```java
PhialOverlay.Builder(app)
    .addPage(customPage)
```
```java
Page customPage = new Page(
    "customPage", // unique page id
    R.drawable.ic_custom_page, // page icon resource
    "Custom page", // page title
    customPageFactory // implementation of PageViewFactory 
);
```

`PageViewFactory` is responsible for instantiating your page view.
Your page view should be implemented as a subclass of `android.view.View` and implement `PageView` interface.

Currently `PageView` is only used for overriding back navigation.
If your page view needs more than a common navigation flow (device back button minimized the overlay) you can implement this logic in `PageView#onBackPressed` method.

## Integration
Usually phial should only be integrated into your internal / debug build .
This can be achieved by creating multiple flavors or build types.
Here is an example with product flavors.
* Create your flavors in `build.gradle` of your application module:
* Create `ApplicationHook` class with same interface in all flavors and put it in the respective source directories for each source set.
* Init Phial in the debug version of `ApplicationHook`. Refer to the sample app for an [example][4].

## Download
1. Add Jitpack maven repository in your root build.gradle file
```groovy
allprojects {
	repositories {
        maven { url "https://jitpack.io" }
    }
}
```

2. Add dependencies *(Assuming that you want to add Phial only to debug build type)*
```groovy
def phialVersion = '<latest-version>'
dependencies {
    debugImplementation "com.github.roshakorost.Phial:phial-overlay:$phialVersion"
    //if you use jira integration
    debugImplementation "com.github.roshakorost.Phial:phial-jira:$phialVersion"
    //if you use html logging 
    debugImplementation "com.github.roshakorost.Phial:phial-logging:$phialVersion"
    //if you use auto fill
    debugImplementation "com.github.roshakorost.Phial:phial-autofill:$phialVersion"
    //if you use key values.
    implementation "com.github.roshakorost.Phial:phial-key-value:$phialVersion"
    //if you use custom scopes.
    implementation "com.github.roshakorost.Phial:phial-scope:$phialVersion"
}
```
*If you use older Android Gradle Plugin change `implementation` to `compile`*

**Note:** in exampel key-values are included into all build types, because you might have a lot of calls `Phial.setKey()` across your application, but without phial-overlay thay will be no operational.

## Feel Free to Contact us
* Rostyslav Roshak - <roshak.rostyslav@gmail.com>
* Andriy Matkovsky - <andriymatkovsky@gmail.com>

[1]:/art/screenshot_demo.gif
[2]:art/data_M11D01_H15_58_53/
[3]:art/screenshot_autofill_demo.gif
[4]:sample/src/qa/java/com/mindcoders/phial/sample/ApplicationHook.java
