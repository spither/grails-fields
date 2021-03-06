package grails.plugin.formfields

import grails.test.mixin.web.GroovyPageUnitTestMixin
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import spock.lang.Specification
import grails.plugin.formfields.mock.*
import grails.test.mixin.*

@TestMixin(GroovyPageUnitTestMixin)
@TestFor(FormFieldsTemplateService)
class FormFieldsTemplateServiceSpec extends Specification {

	Person personInstance
	Employee employeeInstance
	def factory = new BeanPropertyAccessorFactory()
	
	void setup() {
		webRequest.controllerName = "person"

		personInstance = new Person(name: "Bart Simpson", password: "bartman", gender: Gender.Male, dateOfBirth: new Date(87, 3, 19), minor: true)
		personInstance.address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")

		employeeInstance = new Employee(salutation: Salutation.MR, name: "Waylon Smithers", salary: 10)

		factory.grailsApplication = grailsApplication
		factory.constraintsEvaluator = new DefaultConstraintEvaluator()
	}

	void cleanup() {
		views.clear()
		applicationContext.getBean("groovyPagesTemplateEngine").clearPageCache()
		applicationContext.getBean("groovyPagesTemplateRenderer").clearCache()
	}

	void 'uses default template when no others exist'() {
		given:
		views["/fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/fields/default/field'
		template.plugin == null
		template.source.scriptAsString == 'DEFAULT FIELD TEMPLATE'
	}

	void "resolves template for property type"() {
		given:
		views["/fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/fields/string/field'
		template.plugin == null
		template.source.scriptAsString == 'PROPERTY TYPE TEMPLATE'
	}

	void "resolves template for domain class property"() {
		given:
		views["/fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/fields/person/name/field'
		template.plugin == null
		template.source.scriptAsString == 'CLASS AND PROPERTY TEMPLATE'
	}

	void "resolves template from controller views directory"() {
		given:
		views["/fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/person/name/_field.gsp"] = 'CONTROLLER FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/person/name/field'
		template.plugin == null
		template.source.scriptAsString == 'CONTROLLER FIELD TEMPLATE'
	}

	void "does not use controller if there isn't one in the current request"() {
		given:
		views["/fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/name/_field.gsp"] = 'STRANGE TEMPLATE'

		and:
		webRequest.controllerName = null

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/fields/default/field'
		template.plugin == null
		template.source.scriptAsString == 'DEFAULT FIELD TEMPLATE'
	}

	def "resolves template for superclass property"() {
		given:
		views["/fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/fields/person/name/_field.gsp"] = 'SUPERCLASS TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/fields/person/name/field'
		template.plugin == null
		template.source.scriptAsString == 'SUPERCLASS TEMPLATE'
	}

	def "subclass property template overrides superclass property template"() {
		given:
		views["/fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/fields/person/name/_field.gsp"] = 'SUPERCLASS TEMPLATE'
		views["/fields/employee/name/_field.gsp"] = 'SUBCLASS TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/fields/employee/name/field'
		template.plugin == null
		template.source.scriptAsString == 'SUBCLASS TEMPLATE'
	}

	def "property template gets resolved by the property's superclass"() {
		given:
		views["/fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/fields/enum/_field.gsp"] = 'GENERIC ENUM TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'salutation')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/fields/enum/field'
		template.plugin == null
		template.source.scriptAsString == 'GENERIC ENUM TEMPLATE'
	}

	def "property template overrides property's superclass template"() {
		given:
		views["/fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/fields/enum/_field.gsp"] = 'ENUM TEMPLATE'
		views["/fields/salutation/_field.gsp"] = 'SALUTATION TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'salutation')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/fields/salutation/field'
		template.plugin == null
		template.source.scriptAsString == 'SALUTATION TEMPLATE'
	}

	void "resolves template for embedded class property"() {
		given:
		views["/fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/fields/address/city/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'address.city')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/fields/address/city/field'
		template.plugin == null
		template.source.scriptAsString == 'CLASS AND PROPERTY TEMPLATE'
	}

}
