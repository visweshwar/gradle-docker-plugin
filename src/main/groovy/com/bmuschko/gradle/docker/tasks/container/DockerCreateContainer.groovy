/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmuschko.gradle.docker.tasks.container

import com.bmuschko.gradle.docker.tasks.AbstractDockerRemoteApiTask
import com.bmuschko.gradle.docker.utils.CollectionUtil
import groovy.transform.CompileStatic
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

import java.util.concurrent.Callable

class DockerCreateContainer extends AbstractDockerRemoteApiTask {
    @Internal
    final Property<String> imageId = project.objects.property(String)

    @Input
    @Optional
    final ListProperty<String> links = project.objects.listProperty(String)

    @Input
    @Optional
    final Property<String> containerName = project.objects.property(String)

    @Input
    @Optional
    final Property<String> hostName = project.objects.property(String)

    @Input
    @Optional
    final Property<String> ipv4Address = project.objects.property(String)

    @Input
    @Optional
    final ListProperty<String> portSpecs = project.objects.listProperty(String)

    @Input
    @Optional
    final Property<String> user = project.objects.property(String)

    @Input
    @Optional
    final Property<Boolean> stdinOpen = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> stdinOnce = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Long> memory = project.objects.property(Long)

    @Input
    @Optional
    final Property<Long> memorySwap = project.objects.property(Long)

    @Input
    @Optional
    final Property<String> cpuset = project.objects.property(String)

    @Input
    @Optional
    final ListProperty<String> portBindings = project.objects.listProperty(String)

    @Input
    @Optional
    final Property<Boolean> publishAll = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> attachStdin = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> attachStdout = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> attachStderr = project.objects.property(Boolean)

    // use `envVars` instead
    @Deprecated
    @Input
    @Optional
    final ListProperty<String> env = project.objects.listProperty(String)

    @Input
    @Optional
    final Property<Map<?, ?>> envVars = project.objects.property(Map)

    @Input
    @Optional
    final ListProperty<String> cmd = project.objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> entrypoint = project.objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> dns = project.objects.listProperty(String)

    @Input
    @Optional
    final Property<String> network = project.objects.property(String)

    @Input
    @Optional
    final ListProperty<String> networkAliases = project.objects.listProperty(String)

    @Input
    @Optional
    final Property<String> image = project.objects.property(String)

    @Input
    @Optional
    final ListProperty<String> volumes = project.objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> volumesFrom = project.objects.listProperty(String)

    @Input
    @Optional
    final Property<String> workingDir = project.objects.property(String)

    @Input
    final ListProperty<ExposedPort> exposedPorts = project.objects.listProperty(ExposedPort)

    @Input
    @Optional
    final Property<Map<String,String>> binds = project.objects.property(Map)

    @Input
    @Optional
    final ListProperty<String> extraHosts = project.objects.listProperty(String)

    @Input
    @Optional
    final Property<LogConfig> logConfig = project.objects.property(LogConfig)

    @Input
    @Optional
    final Property<Boolean> privileged = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> tty = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<String> restartPolicy = project.objects.property(String)

    @Input
    @Optional
    final Property<String> pid = project.objects.property(String)

    @Input
    @Optional
    final ListProperty<String> devices = project.objects.listProperty(String)

    /**
     * Size of <code>/dev/shm</code> in bytes.
     * The size must be greater than 0.
     * If omitted the system uses 64MB.
     */
    @Input
    @Optional
    final Property<Long> shmSize = project.objects.property(Long)

    /* 
     * Automatically remove the container when the container's process exits.
     *
     * This has no effect if {@link #restartPolicy} is set.
     * @since 3.6.2
     */
    @Input
    @Optional
    final Property<Boolean> autoRemove= project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Map<String, String>> labels = project.objects.property(Map)

    @Internal
    final Property<String> containerId = project.objects.property(String)

    @Input
    @Optional
    final Property<String> macAddress = project.objects.property(String)

    @Override
    void runRemoteCommand(dockerClient) {
        def containerCommand = dockerClient.createContainerCmd(imageId.get())
        setContainerCommandConfig(containerCommand)
        def container = containerCommand.exec()
        final String localContainerName = containerName.getOrNull() ?: container.id
        logger.quiet "Created container with ID '$localContainerName'."
        containerId.set(container.id)
        if(onNext) {
            onNext.call(container)
        }
    }

    void targetImageId(String imageId) {
        this.imageId.set(imageId)
    }

    @CompileStatic
    void targetImageId(Callable<String> imageId) {
        targetImageId(project.provider(imageId))
    }

    void targetImageId(Provider<String> imageId) {
        this.imageId.set(imageId)
    }

    void logConfig(String type, Map<String, String> config) {
        this.logConfig.set(new LogConfig(type: type, config: config))
    }

    void exposePorts(String internetProtocol, List<Integer> ports) {
        exposedPorts.add(new ExposedPort(internetProtocol, ports))
    }

    void restartPolicy(String name, int maximumRetryCount) {
        this.restartPolicy.set("${name}:${maximumRetryCount}".toString())
    }

    // key or value can be in the form of a Closure or anything else. In the
    // end, and whatever it resolves to, will be marshaled into a String.
    void withEnvVar(def key, def value) {
        if (envVars.getOrNull()) {
            envVars.get().put(key, value)
        } else {
            envVars.set([(key): value])
        }
    }

    private void setContainerCommandConfig(containerCommand) {
        if(containerName.getOrNull()) {
            containerCommand.withName(containerName.get())
        }

        if(hostName.getOrNull()) {
            containerCommand.withHostName(hostName.get())
        }

        if(ipv4Address.getOrNull()){
            containerCommand.withIpv4Address(ipv4Address.get())
        }

        if(portSpecs.getOrNull()) {
            containerCommand.withPortSpecs(portSpecs.get())
        }

        if(user.getOrNull()) {
            containerCommand.withUser(user.get())
        }

        if(stdinOpen.getOrNull()) {
            containerCommand.withStdinOpen(stdinOpen.get())
        }

        if(stdinOnce.getOrNull()) {
            containerCommand.withStdInOnce(stdinOnce.get())
        }

        if(memory.getOrNull()) {
            containerCommand.withMemory(memory.get())
        }

        if(memorySwap.getOrNull()) {
            containerCommand.withMemorySwap(memorySwap.get())
        }

        if(cpuset.getOrNull()) {
            containerCommand.withCpusetCpus(cpuset.get())
        }

        if(attachStdin.getOrNull()) {
            containerCommand.withAttachStdin(attachStdin.get())
        }

        if(attachStdout.getOrNull()) {
            containerCommand.withAttachStdout(attachStdout.get())
        }

        if(attachStderr.getOrNull()) {
            containerCommand.withAttachStderr(attachStderr.get())
        }

        // marshall deprecated old list onto new map
        env.getOrNull()?.each { envVar ->
            def keyValuePair = envVar.split('=', 2)
            envVars.get().put(keyValuePair.first(), keyValuePair.last())
        }

        // marshall map into list
        if(envVars.getOrNull()) {
            final List<String> localEnvVars = new ArrayList<>();
            envVars.get().each { key, value ->
                def localKey = key instanceof Closure ? key.call() : key
                def localValue = value instanceof Closure ? value.call() : value

                localEnvVars.add("${localKey}=${localValue}".toString())
            }
            containerCommand.withEnv(localEnvVars)
        }

        if(cmd.getOrNull()) {
            containerCommand.withCmd(cmd.get())
        }

        if(entrypoint.getOrNull()) {
            containerCommand.withEntrypoint(entrypoint.get())
        }

        if(dns.getOrNull()) {
            containerCommand.withDns(dns.get())
        }

        if(network.getOrNull()) {
            containerCommand.withNetworkMode(network.get())
        }

        if(networkAliases.getOrNull()) {
            containerCommand.withAliases(networkAliases.get())
        }

        if(image.getOrNull()) {
            containerCommand.withImage(image.get())
        }

        if(volumes.getOrNull()) {
            def createdVolumes = volumes.get().collect { threadContextClassLoader.createVolume(it) }
            containerCommand.volumes = threadContextClassLoader.createVolumes(createdVolumes)
        }

        if (links.getOrNull()) {
            def createdLinks = links.get().collect { threadContextClassLoader.createLink(it) }
            containerCommand.withLinks(CollectionUtil.toArray(createdLinks))
        }

        if(volumesFrom.getOrNull()) {
            def createdVolumes = threadContextClassLoader.createVolumesFrom(volumesFrom.get() as String[])
            containerCommand.withVolumesFrom(createdVolumes)
        }

        if(workingDir.getOrNull()) {
            containerCommand.withWorkingDir(workingDir.get())
        }

        if(exposedPorts.getOrNull()) {
            def ports = threadContextClassLoader.createExposedPortsArray(exposedPorts.get())
            containerCommand.withExposedPorts(ports)
        }

        if(portBindings.getOrNull()) {
            def createdPortBindings = portBindings.get().collect { threadContextClassLoader.createPortBinding(it) }
            containerCommand.withPortBindings(threadContextClassLoader.createPorts(createdPortBindings))
        }

        if(publishAll.getOrNull()) {
            containerCommand.withPublishAllPorts(publishAll.get())
        }

        if(binds.getOrNull()) {
            def createdBinds = threadContextClassLoader.createBinds(binds.get())
            containerCommand.withBinds(createdBinds)
        }

        if(extraHosts.getOrNull()) {
            containerCommand.withExtraHosts(extraHosts.get() as String[])
        }

        if(logConfig.getOrNull()) {
            containerCommand.withLogConfig(threadContextClassLoader.createLogConfig(logConfig.get().type, logConfig.get().config))
        }

        if(privileged.getOrNull()) {
            containerCommand.withPrivileged(privileged.get())
        }

        if (restartPolicy.getOrNull()) {
            containerCommand.withRestartPolicy(threadContextClassLoader.createRestartPolicy(restartPolicy.get()))
        }

        if (pid.getOrNull()) {
            containerCommand.withPidMode(pid.get())
        }

        if (devices.getOrNull()) {
            def createdDevices = devices.get().collect { threadContextClassLoader.createDevice(it) }
            containerCommand.withDevices(CollectionUtil.toArray(createdDevices))
        }

        if(tty.getOrNull()) {
            containerCommand.withTty(tty.get())
        }

        if(shmSize.getOrNull() != null) { // 0 is valid input
            containerCommand.hostConfig.withShmSize(shmSize.get())
        }

        if (autoRemove.getOrNull()) {
            containerCommand.hostConfig.withAutoRemove(autoRemove.get())
        }

        if(labels.getOrNull()) {
            containerCommand.withLabels(labels.get().collectEntries { [it.key, it.value.toString()] })
        }

        if(macAddress.getOrNull()) {
            containerCommand.withMacAddress(macAddress.get())
        }
    }

    static class LogConfig {
        @Input String type
        @Input Map<String, String> config = [:]
    }

    static class ExposedPort {
        @Input final String internetProtocol
        @Input final List<Integer> ports = []

        ExposedPort(String internetProtocol, List<Integer> ports) {
            this.internetProtocol = internetProtocol
            this.ports = ports
        }
    }
}

