# 🔐 BombActivationDecoder

A Java-based implementation inspired by **Shamir's Secret Sharing Algorithm**, designed to securely reconstruct a secret (hypothetically used to activate a bomb) from mathematical shares represented in multiple bases.

---

## 📜 Project Description

This project reads input from a JSON file that provides:
- `N` total encoded shares (with `x` and `y` values)
- Mathematical encodings like base-10, base-16, etc.
- A minimum of `K` valid shares required to reconstruct the secret (where `K < N`)

The goal is to:
- Decode the shares
- Choose any `K` correct shares
- Use **Lagrange Interpolation** to reconstruct the constant coefficient `C` of the secret polynomial
- Detect and report any **incorrect or corrupted shares**
- Handle **very large numbers** with precision using Java's `BigInteger`

---

## 📁 Folder Structure

hashira/
├── BombActivationDecoder.java
├── testcase1.json
├── testcase2.json
└── README.md


---

## 🔧 Features

- 🔢 Supports arbitrary base decoding (binary, octal, decimal, hex)
- ➗ Uses Lagrange Interpolation to recover secrets from partial shares
- 🧠 Detects incorrect or corrupted shares during reconstruction
- 💡 Handles large numbers via `BigInteger`
- 📄 Works without any external libraries — lightweight custom JSON parser included
- ✅ Verifies consistency across multiple combinations

---

## 🧪 Sample Input JSON Format

### ✅ testcase1.json
```json
{
  "keys": { "n": 5, "k": 3 },
  "1": { "base": "10", "value": "12345" },
  "2": { "base": "16", "value": "1a2b" },
  "3": { "base": "10", "value": "67890" },
  "4": { "base": "8",  "value": "1573" },
  "5": { "base": "2",  "value": "101011" }
}
