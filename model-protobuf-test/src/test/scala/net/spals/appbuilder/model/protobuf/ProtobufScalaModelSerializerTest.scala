package net.spals.appbuilder.model.protobuf

import net.spals.appbuilder.message.protobuf.addressbookv2.PersonV2.{PhoneNumberV2, PhoneTypeV2}
import net.spals.appbuilder.message.protobuf.addressbookv2.{AddressBookV2, PersonV2}
import net.spals.appbuilder.message.protobuf.addressbookv3.PersonV3.{PhoneNumberV3, PhoneTypeV3}
import net.spals.appbuilder.message.protobuf.addressbookv3.{AddressBookV3, PersonV3}
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.{is, notNullValue}
import org.testng.annotations.{DataProvider, Test}


/**
  * Unit tests for [[ProtobufModelSerializer]]
  *
  * @author tkral
  */
class ProtobufScalaModelSerializerTest {

  @DataProvider
  def modelEqualityProvider(): Array[Array[AnyRef]] = {
    val personV2_1 = PersonV2(id = 1, name = "Tim")
    val personV2_2 = PersonV2(id = 2, name = "Jim", email = Option("jim@spals.net"),
      phones = Seq(PhoneNumberV2(number = "123-456-7890", `type` = Option(PhoneTypeV2.MOBILE))))

    val personV3_1 = PersonV3(id = 3, name = "Tim")
    val personV3_2 = PersonV3(id = 4, name = "Jim", email = "jim@spals.net",
      phones = Seq(PhoneNumberV3(number = "123-456-7890", `type` = PhoneTypeV3.MOBILE)))

    Array(
      Array(personV2_1),
      Array(personV2_2),
      Array(AddressBookV2(people = Seq(personV2_1, personV2_2))),

      Array(personV3_1),
      Array(personV3_2),
      Array(AddressBookV3(people = Seq(personV3_1, personV3_2)))
    )
  }

  @Test(dataProvider = "modelEqualityProvider")
  def testModelEquality(modelObject: Any) {
    val modelSerializer = new ProtobufModelSerializer

    val bytes = modelSerializer.serialize(modelObject)
    assertThat(bytes, is(notNullValue))

    val deserializedModelObject = modelSerializer.deserialize(bytes)
    assertThat(deserializedModelObject, is(modelObject))
  }

//  @Test(dataProvider = "modelEqualityProvider")
  def testModelEqualityJson(modelObject: Any): Unit = {
    val modelSerializer = new ProtobufModelSerializer

    val json = modelSerializer.jsonSerialize(modelObject)
    assertThat(json, is(notNullValue))

    val deserializedModelObject = modelSerializer.jsonDeserialize(json)
    assertThat(deserializedModelObject, is(modelObject))
  }
}
