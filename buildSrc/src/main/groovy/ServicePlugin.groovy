import org.gradle.api.Plugin
import org.gradle.api.Project

class ServicePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.apply(plugin: 'org.springframework.boot')
    	project.apply(plugin: "io.spring.dependency-management")

        project.dependencyManagement {
            imports {
                mavenBom "org.springframework.cloud:spring-cloud-starter-sleuth:${project.ext.springCloudSleuthVersion}"
            }
        }


        project.dependencies {

            compile 'org.springframework.cloud:spring-cloud-starter-sleuth'
            compile 'org.springframework.cloud:spring-cloud-starter-zipkin:2.2.8.RELEASE'
            compile 'io.zipkin.brave:brave-bom:4.17.1'

            compile "io.eventuate.tram.core:eventuate-tram-spring-cloud-sleuth-integration"
        }

    }
}
