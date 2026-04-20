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

type ProviderStatus struct {
	ProviderName   string `json:"providerName"`
	Available      bool   `json:"available"`
	TotalKeys      int    `json:"totalKeys"`
	AvailableKeys  int    `json:"availableKeys"`
	RateLimitedKeys int   `json:"rateLimitedKeys"`
	InactiveKeys   int    `json:"inactiveKeys"`
}

type GatewayStatus struct {
	Healthy   bool             `json:"healthy"`
	Providers []ProviderStatus `json:"providers"`
}