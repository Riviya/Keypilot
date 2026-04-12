// client/gateway_client.go
package client

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/Riviya/Keypilot/cli/model"
	
)

// GatewayClient handles all HTTP communication with the Spring Boot gateway.
// It is the only place in the CLI that knows about HTTP.
type GatewayClient struct {
	baseURL    string
	httpClient *http.Client
}

// NewGatewayClient constructs a client pointed at the given base URL.
// In production this will be http://localhost:4000.
// In tests, httptest.NewServer().URL is passed instead.
func NewGatewayClient(baseURL string) *GatewayClient {
	return &GatewayClient{
		baseURL:    baseURL,
		httpClient: &http.Client{},
	}
}

// AddKey posts a new API key to the gateway.
func (c *GatewayClient) AddKey(provider, keyValue string) (*model.ApiKeyResponse, error) {
	reqBody := model.AddApiKeyRequest{
		Provider: provider,
		KeyValue: keyValue,
	}

	bodyBytes, err := json.Marshal(reqBody)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize request: %w", err)
	}

	resp, err := c.httpClient.Post(
		c.baseURL+"/api/keys",
		"application/json",
		bytes.NewBuffer(bodyBytes),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to reach gateway: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusCreated {
		return nil, fmt.Errorf("gateway returned status %d", resp.StatusCode)
	}

	var response model.ApiKeyResponse
	if err := json.NewDecoder(resp.Body).Decode(&response); err != nil {
		return nil, fmt.Errorf("failed to parse gateway response: %w", err)
	}

	return &response, nil
}

// ListKeys fetches all stored API keys from the gateway.
func (c *GatewayClient) ListKeys() ([]model.ApiKeyResponse, error) {
	resp, err := c.httpClient.Get(c.baseURL + "/api/keys")
	if err != nil {
		return nil, fmt.Errorf("failed to reach gateway: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("gateway returned status %d", resp.StatusCode)
	}

	var keys []model.ApiKeyResponse
	if err := json.NewDecoder(resp.Body).Decode(&keys); err != nil {
		return nil, fmt.Errorf("failed to parse gateway response: %w", err)
	}

	return keys, nil
}

// DeleteKey removes an API key by ID from the gateway.
func (c *GatewayClient) DeleteKey(id string) error {
	req, err := http.NewRequest(
		http.MethodDelete,
		fmt.Sprintf("%s/api/keys/%s", c.baseURL, id),
		nil,
	)
	if err != nil {
		return fmt.Errorf("failed to build request: %w", err)
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("failed to reach gateway: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusNoContent {
		return fmt.Errorf("gateway returned status %d", resp.StatusCode)
	}

	return nil
}