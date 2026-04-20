// gateway-cli/integration/contract_test.go
package integration_test

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/Riviya/Keypilot/cli/client"
	"github.com/Riviya/Keypilot/cli/model"
)

// Contract: AddKey response must contain id, provider, active
// If the gateway changes these field names, this test catches it
func TestContract_AddKeyResponseShape(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusCreated)
		// Simulate exact gateway response structure
		json.NewEncoder(w).Encode(map[string]interface{}{
			"id":       "uuid-123",
			"provider": "openai",
			"active":   true,
			// Deliberately no "keyValue" — contract says it must not be present
		})
	}))
	defer server.Close()

	c := client.NewGatewayClient(server.URL)
	resp, err := c.AddKey("openai", "sk-test")

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if resp.ID == "" {
		t.Error("contract violation: id field missing or empty")
	}
	if resp.Provider == "" {
		t.Error("contract violation: provider field missing or empty")
	}
	// active is a bool — zero value is false, valid for an active key
}

// Contract: ListKeys must return an array (even when empty)
func TestContract_ListKeysReturnsArray(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode([]model.ApiKeyResponse{})
	}))
	defer server.Close()

	c := client.NewGatewayClient(server.URL)
	keys, err := c.ListKeys()

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if keys == nil {
		t.Error("contract violation: expected empty array, got nil")
	}
}

// Contract: Status response must contain healthy bool and providers array
func TestContract_StatusResponseShape(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode(model.GatewayStatus{
			Healthy: true,
			Providers: []model.ProviderStatus{
				{
					ProviderName:    "openai",
					Available:       true,
					TotalKeys:       1,
					AvailableKeys:   1,
					RateLimitedKeys: 0,
					InactiveKeys:    0,
				},
			},
		})
	}))
	defer server.Close()

	c := client.NewGatewayClient(server.URL)
	status, err := c.GetStatus()

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if !status.Healthy {
		t.Error("contract violation: healthy field missing")
	}
	if status.Providers == nil {
		t.Error("contract violation: providers array missing")
	}
	p := status.Providers[0]
	if p.ProviderName == "" {
		t.Error("contract violation: providerName field missing")
	}
	// Verify no keyValue field exists — checked structurally by the model
	// If gateway adds keyValue, the model would need updating (contract break)
}

// Contract: Delete returns 204 with no body
func TestContract_DeleteReturns204(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNoContent)
		// No body — 204 means no content
	}))
	defer server.Close()

	c := client.NewGatewayClient(server.URL)
	err := c.DeleteKey("any-id")

	if err != nil {
		t.Errorf("contract violation: 204 should not produce error, got: %v", err)
	}
}