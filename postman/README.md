# Lagos Rent — Postman Integration Guide

This directory contains the ready-to-use Postman collection and environment files for the Lagos Rent distributed platform.

## Files
- `lagos-rent.postman_collection.json`: The complete API collection covering all implemented services.
- `lagos-rent.postman_environment.json`: Local development environment variables.

---

## 1. How to Import into Postman

1. Open the **Postman** desktop application on your Mac.
2. In the top-left corner, click **Import** (or press `Cmd + O`).
3. Select or drag-and-drop both files from your project directory:
   - `/Users/user/Desktop/LAGOS_RENT/postman/lagos-rent.postman_collection.json`
   - `/Users/user/Desktop/LAGOS_RENT/postman/lagos-rent.postman_environment.json`
4. In the top-right corner of Postman, switch the active environment dropdown to **`Lagos Rent — Local`**.

---

## 2. Automatic Chaining (How It Works)

You don't need to manually copy-paste JWT tokens or IDs:

1. **Authentication (`01 - Authentication (Keycloak)`)**:
   - Run **Landlord Login**, **Tenant Login**, or **Admin Login**.
   - The test script automatically saves `{{landlord_token}}`, `{{tenant_token}}`, or `{{admin_token}}` directly into your Postman environment.
2. **Resource IDs**:
   - Creating a Property automatically saves `{{property_id}}`.
   - Adding a Unit automatically saves `{{unit_id}}`.
   - Creating a Listing automatically saves `{{listing_id}}`.
   - Fetching Listing version automatically saves `{{listing_version}}` for Admin approval optimistic locking.
   - Applications and Payables automatically store `{{application_id}}` and `{{payable_id}}`.

---

## 3. Moving or Copying Files Between Postman, IntelliJ, and Antigravity

- **From Postman back to your codebase**:
  If you add new endpoints or tweak requests in Postman, click the collection's `...` menu $\rightarrow$ **Export** $\rightarrow$ overwrite `lagos-rent.postman_collection.json` in this folder. IntelliJ and Antigravity will instantly see the update.
- **From Antigravity / IntelliJ to Postman**:
  Whenever a new Phase is built or an endpoint changes, Antigravity can update `lagos-rent.postman_collection.json`. Then, simply click **Import** in Postman and choose "Replace" to get the latest endpoints.
