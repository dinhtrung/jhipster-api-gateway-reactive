import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ApiGatewaySharedModule } from 'app/shared/shared.module';
import { HOME_ROUTE } from './home.route';
import { HomeComponent } from './home.component';

@NgModule({
  imports: [ApiGatewaySharedModule, RouterModule.forChild([HOME_ROUTE])],
  declarations: [HomeComponent]
})
export class ApiGatewayHomeModule {}
