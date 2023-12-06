def call (String giturl, String gitBranch) {
    checkout([
        $class: 'GitSCM',
        branches: [[name:gitBranch]],
        userRemoteConfigs: [[url: giturl]]
    ])
}