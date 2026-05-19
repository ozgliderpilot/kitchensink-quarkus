db.createCollection("member");

db.member.createIndex(
    { email: 1 },
    { unique: true, name: "emailUniqIdx" }
);

db.member.insertOne({
    _id: ObjectId("6652c45dc4b9f77f03cc3c9f"),
    name: "John Smith",
    email: "john.smith@mailinator.com",
    phoneNumber: "2125551212"
});
