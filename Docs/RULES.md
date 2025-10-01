# 📋 **DEVELOPMENT RULES & PROCEDURES**

## 🎯 **Project Development Rules**

create md overall  the project and inside it refrence for all of this documant  
also for archticte that will updated after add , update delete fun , future from the arch 

### **1. CHECKLIST PROCEDURE**
- [ ] **Before Development**: Check Git remote connection
- [ ] **Before Coding**: Verify development environment setup
- [ ] **Before Build**: Check required dependencies
- [ ] **Before Push**: Ensure successful build and testing
- [ ] **After Changes**: Update documentation and logs
- [ ] **After Issues**: Log all attempts and solutions

### **2. MARKDOWN DOCUMENTATION FOR FUTURE**
- [ ] **Create MD file** for every new feature/component
- [ ] **Update existing MD** files when modifying features
- [ ] **Include usage examples** and integration guides
- [ ] **Document all parameters** and return values
- [ ] **Add troubleshooting** sections for common issues
- [ ] **Include performance** considerations and best practices

### **3. INTEGRATION PROCEDURES**
- [ ] **Test integration** with existing components
- [ ] **Verify compatibility** with current architecture
- [ ] **Check for conflicts** with existing code
- [ ] **Update dependencies** if needed
- [ ] **Test cross-platform** compatibility
- [ ] **Validate UI/UX** consistency

### **4. HISTORICAL LOGGING**
- [ ] **Log every session** in DEVELOPMENT_LOG.md
- [ ] **Update HISTORICAL_DEVELOPMENT_LOG.md** with summaries
- [ ] **Record all issues** in ISSUE_LOG.md
- [ ] **Document all solutions** attempted (successful and failed)
- [ ] **Track performance** metrics and improvements
- [ ] **Maintain timeline** of all development activities

### **5. CODING CONCEPTS - AGILE & DEVOPS**
- [ ] **Small Code Principle**: One function per file
- [ ] **Single Responsibility**: Each component has one purpose
- [ ] **OOP Principles**: Follow SOLID principles
- [ ] **Error Prevention**: Avoid previous compilation issues
- [ ] **Incremental Development**: Small, testable changes
- [ ] **Continuous Integration**: Test after each change

### **6. BUILD & DEBUG PROCEDURES**
- [ ] **Always test build** before pushing
- [ ] **Run `./gradlew build`** to check compilation
- [ ] **Run `./gradlew assembleDebug`** for debug build
- [ ] **Check for warnings** and resolve if critical
- [ ] **Test on target device** if possible
- [ ] **Monitor build performance** and optimize

### **7. TERMINAL HANDLING**
- [ ] **If "Terminate batch job (Y/N)?" appears**: Press Ctrl+C twice
- [ ] **If build hangs**: Wait 30 seconds then Ctrl+C
- [ ] **If PowerShell errors**: Check command syntax
- [ ] **If Git issues**: Verify remote connection
- [ ] **If permission errors**: Run as administrator if needed

### **8. SUCCESS/FAILURE HANDLING**
- [ ] **If build SUCCESS**: Proceed with testing and push
- [ ] **If build FAILS**: 
  - [ ] Log all error messages
  - [ ] Document all solution attempts
  - [ ] Try multiple approaches
  - [ ] Update issue log with findings
  - [ ] Fix before proceeding

### **9. MONITORING & DEBUGGING**
- [ ] **Run PowerShell monitoring**: `powershell -ExecutionPolicy Bypass -File monitor_debug_simple.ps1`
- [ ] **Monitor app performance** during testing
- [ ] **Check memory usage** and CPU utilization
- [ ] **Monitor network** activity if applicable
- [ ] **Log performance** metrics for optimization

### **10. APP TESTING & DEPLOYMENT**
- [ ] **Install app**: Build and install on target device
- [ ] **Launch app**: `"$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s 192.168.8.20:5555 shell am start -n com.example.iptvtv/.HomeScreen`
- [ ] **Test functionality** thoroughly
- [ ] **Monitor for crashes** or errors
- [ ] **Test user interactions** and UI responsiveness
- [ ] **Verify all features** work as expected

### **11. PUSH PROCEDURES**
- [ ] **ONLY push after user confirmation**
- [ ] **Ensure all tests pass** before pushing
- [ ] **Update all documentation** before push
- [ ] **Use conventional commit** messages
- [ ] **Push to correct branch** (SpeedMenuaOveraly)
- [ ] **Verify push success** and remote sync

---

## 🔧 **TECHNICAL RULES**

### **Code Quality Standards**
- **Small Functions**: Maximum 50 lines per function
- **Single Purpose**: Each file has one clear responsibility
- **Error Handling**: Always implement proper error handling
- **Resource Cleanup**: Always release resources properly
- **Documentation**: Comment all public functions and classes
- **Testing**: Test each component individually

### **Git Workflow**
- **Branch Strategy**: Use feature branches for new development
- **Commit Messages**: Use conventional commit format
- **Commit Frequency**: Commit after each working feature
- **Push Policy**: Only push after user confirmation
- **Merge Strategy**: Use pull requests for major changes

### **Build Process**
- **Pre-build Check**: Verify all dependencies
- **Build Command**: `./gradlew build`
- **Debug Build**: `./gradlew assembleDebug`
- **Release Build**: `./gradlew assembleRelease`
- **Error Handling**: Log all build errors and solutions

### **Testing Requirements**
- **Unit Testing**: Test individual components
- **Integration Testing**: Test component interactions
- **UI Testing**: Test user interface functionality
- **Performance Testing**: Monitor app performance
- **Device Testing**: Test on target devices

---

## 📊 **MONITORING & LOGGING**

### **Performance Monitoring**
- **Memory Usage**: Monitor for memory leaks
- **CPU Usage**: Track CPU utilization
- **Network Activity**: Monitor network requests
- **Battery Usage**: Track battery consumption
- **App Launch Time**: Monitor startup performance

### **Error Logging**
- **Build Errors**: Log all compilation errors
- **Runtime Errors**: Log all app crashes
- **User Feedback**: Log user-reported issues
- **Performance Issues**: Log performance problems
- **Integration Issues**: Log component conflicts

### **Documentation Updates**
- **Code Changes**: Update relevant documentation
- **New Features**: Create new documentation files
- **Bug Fixes**: Update issue logs with solutions
- **Performance Improvements**: Document optimizations
- **User Feedback**: Update user guides

---

## 🚨 **EMERGENCY PROCEDURES**

### **Build Failures**
1. **Stop development** immediately
2. **Log all error messages** in issue log
3. **Try multiple solutions** and document each
4. **Revert to last working version** if needed
5. **Notify user** of issues and solutions
6. **Resume only after** successful build

### **App Crashes**
1. **Collect crash logs** and stack traces
2. **Identify root cause** of crash
3. **Implement fix** and test thoroughly
4. **Update issue log** with crash details
5. **Test on multiple devices** if possible
6. **Monitor for recurrence** of crash

### **Performance Issues**
1. **Profile app performance** using tools
2. **Identify bottlenecks** in code
3. **Implement optimizations** gradually
4. **Test performance improvements**
5. **Monitor for regressions**
6. **Document optimization strategies**

---

## 📋 **DAILY CHECKLIST**

### **Morning Routine**
- [ ] **Check Git status** and remote connection
- [ ] **Review pending issues** and tasks
- [ ] **Update development environment** if needed
- [ ] **Plan daily development** goals
- [ ] **Check project documentation** for updates

### **Development Session**
- [ ] **Follow coding standards** and principles
- [ ] **Test each change** before proceeding
- [ ] **Update documentation** as you code
- [ ] **Log all issues** and solutions
- [ ] **Monitor performance** during development

### **End of Day**
- [ ] **Commit all changes** with proper messages
- [ ] **Update all logs** and documentation
- [ ] **Test final build** before finishing
- [ ] **Plan next session** goals
- [ ] **Backup important changes** if needed

---

**Rules Created**: December 2024  
**Last Updated**: December 2024  
**Status**: ✅ **ACTIVE ENFORCEMENT**
