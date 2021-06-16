
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import ProductManager from "./components/ProductManager"

import OrderManager from "./components/OrderManager"

import DeliveryManager from "./components/DeliveryManager"
import CancellationManager from "./components/CancellationManager"

export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [
            {
                path: '/Product',
                name: 'ProductManager',
                component: ProductManager
            },

            {
                path: '/Order',
                name: 'OrderManager',
                component: OrderManager
            },

            {
                path: '/Delivery',
                name: 'DeliveryManager',
                component: DeliveryManager
            },
            {
                path: '/Cancellation',
                name: 'CancellationManager',
                component: CancellationManager
            },



    ]
})
