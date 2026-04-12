// model/api_key.go
package model

// AddApiKeyRequest mirrors the gateway's POST /api/keys body
type AddApiKeyRequest struct {
	Provider string `json:"provider"`
	KeyValue string `json:"keyValue"`
}

// ApiKeyResponse mirrors the gateway's response DTO
type ApiKeyResponse struct {
	ID       string `json:"id"`
	Provider string `json:"provider"`
	Active   bool   `json:"active"`
}