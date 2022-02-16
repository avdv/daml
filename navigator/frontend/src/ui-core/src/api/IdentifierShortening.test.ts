// Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates.
// All rights reserved.

import { shortenContractId, shortenPartyId, shortenTemplateId } from "./IdentifierShortening";

test("shortenContractId doesn't shorten short strings", () => {
  expect(shortenContractId("short")).toBe("short");
});

test("shortenContractId shortens longer strings", () => {
  expect(shortenContractId("very_long_identifier")).toBe("very_long_id...");
});

test("shortenContractId behaves as expected at the boundary between short and long identifiers", () => {
  expect(shortenContractId("123456789012")).toBe("123456789012"); // exactly 12
  expect(shortenContractId("1234567890123")).toBe("123456789012..."); // exactly 13
});

test("shortenPartyId doesn't shorten short strings", () => {
  expect(shortenPartyId("short")).toBe("short");
});

test("shortenPartyId shortens longer strings", () => {
  expect(shortenPartyId("very_long_identifier")).toBe("very_long_id...");
});

test("shortenPartyId behaves as expected at the boundary between short and long identifiers", () => {
  expect(shortenPartyId("123456789012")).toBe("123456789012"); // exactly 12
  expect(shortenPartyId("1234567890123")).toBe("123456789012..."); // exactly 13
});

test("shortenPartyId ignores the prefix when shortening", () => {
  expect(shortenPartyId("very_long_identifier::short")).toBe("very_long_identifier::short");
  expect(shortenPartyId("very_long_identifier::very_long_namespace")).toBe("very_long_identifier::very_long_na...");
});

test("shortenTemplateId doesn't shorten short strings", () => {
  expect(shortenTemplateId("short")).toBe("short");
});

test("shortenTemplateId shortens longer strings", () => {
  expect(shortenTemplateId("very_long_identifier")).toBe("very_long_id...");
});

test("shortenTemplateId behaves as expected at the boundary between short and long identifiers", () => {
  expect(shortenTemplateId("123456789012")).toBe("123456789012"); // exactly 12
  expect(shortenTemplateId("1234567890123")).toBe("123456789012..."); // exactly 13
});

test("shortenTemplateId ignores the prefix when shortening", () => {
  expect(shortenTemplateId("very_long:identifier@short")).toBe("very_long:identifier@short");
  expect(shortenTemplateId("very_long:identifier@very_long_namespace")).toBe("very_long:identifier@very_long_na...");
});
